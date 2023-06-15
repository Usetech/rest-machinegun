package com.usetech.rest_machinegun;

import com.usetech.rest_machinegun.config.MachinegunTask;
import com.usetech.rest_machinegun.config.MachinegunTaskAuthorization;
import com.usetech.rest_machinegun.utils.patterns.*;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.impl.future.CompositeFutureImpl;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class MachinegunVerticle extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);
	public static final String MACHINEGUN_REQUEST_EVENT_ADDRESS = "machinegun.task.request";
	private static final Map<String, PlaceholderResolver> resolversCache = new HashMap<>();
	private static CounterHelper counterHelper;
	private static RandomHelper randomHelper;

	WebClient client;

	public static void sendTaskEvent(Vertx vertx, MachinegunTask task, boolean immediately) {
		if (task.getIntervalMs() == null) return;
		final JsonObject obj = JsonObject.mapFrom(task);
		if (immediately) {
			vertx.eventBus().send(MACHINEGUN_REQUEST_EVENT_ADDRESS, obj);
		} else {
			String intervalStr = resolveString(task, task.getIntervalMs());
			long interval = Long.parseLong(intervalStr);
			vertx.setTimer(interval, l -> vertx.eventBus().send(MACHINEGUN_REQUEST_EVENT_ADDRESS, obj));
		}
	}

	@Override
	public void start() {
		// web client
		client = WebClient.create(vertx);

		// helpers
		counterHelper = new CounterHelper(vertx);
		randomHelper = new RandomHelper();

		// register event consumer
		vertx.eventBus().consumer(MACHINEGUN_REQUEST_EVENT_ADDRESS, handleRequestEvent());
	}

	@Override
	public void stop() {
		resolversCache.clear();
	}

	private static PlaceholderResolver getResolver(String pattern) {
		return resolversCache.computeIfAbsent(pattern, p -> {
			PlaceholderResolver resolver = MustacheResolver.hasPlaceholder(p)
				? new MustacheResolver()
				: new NullResolver();
			resolver.compile(p);
			return resolver;
		});
	}

	private static String placeholderResolver(MachinegunTask task, String placeholder) {
		if (placeholder == null) return null;

		String[] parts = placeholder.split("\\.");
		if (parts.length == 0) return null;

		if ("counter".equalsIgnoreCase(parts[0])) { // counter
			if (parts.length > 1)
				return Long.valueOf(counterHelper.incrementAndGetCounterValue(parts[1])).toString();
			return Long.valueOf(counterHelper.incrementAndGetGlobalCounterValue()).toString();
		}

		if ("random".equalsIgnoreCase(parts[0])) { // random
			return randomHelper.random(parts);
		}

		if ("env".equalsIgnoreCase(parts[0])) { // env variable
			if (parts.length == 1)
				return null;
			return task.getEnv().getOrDefault(parts[1], System.getenv(parts[1]));
		}

		return null;
	}

	private static String resolveString(MachinegunTask task, String pattern) {
		if (pattern == null) return null;
		PlaceholderResolver resolver = getResolver(pattern);
		return resolver.resolve(p -> {
			if (p == null || p.isEmpty()) return "";
			String res = placeholderResolver(task, p);
			return res == null ? "" : res;
		});
	}


	private Future<HttpRequest<Buffer>> authorizeRequest(HttpRequest<Buffer> request, MachinegunTask task) {
		MachinegunTaskAuthorization authConfig = task.getAuthorization();
		if (authConfig == null) return Future.failedFuture("Null authorization");

		if (authConfig.getOauth2() != null) {
			OAuth2Auth oauth2 = OAuth2Auth.create(vertx, new OAuth2Options()
				.setFlow(OAuth2FlowType.PASSWORD)
				.setTokenPath(resolveString(task, authConfig.getOauth2().getTokenUrl()))
				.setJwkPath(resolveString(task, authConfig.getOauth2().getJwkUrl()))
				.setClientId(resolveString(task, authConfig.getOauth2().getClientId()))
				.setClientSecret(resolveString(task, authConfig.getOauth2().getClientSecret()))
			);
			Future<User> authFuture = oauth2.authenticate(new UsernamePasswordCredentials(
				resolveString(task, authConfig.getOauth2().getUsername()),
				resolveString(task, authConfig.getOauth2().getPassword())
			));
			return authFuture.map(usr -> {
				logger.debug("Authorized successfully");
				request.bearerTokenAuthentication(usr.principal().getString("access_token"));
				return request;
			});

		}
		if (authConfig.getBasic() != null) {
			request.basicAuthentication(authConfig.getBasic().getUsername(), authConfig.getBasic().getPassword());
			return Future.succeededFuture(request);
		}
		return Future.failedFuture("Undefined authorization");
	}

	private String refreshVolatileRequest(HttpRequest<Buffer> request, MachinegunTask task) {
		// request
		String url = resolveString(task, task.getRequest().getUrl());
		RequestOptions reqOpt = new RequestOptions().setAbsoluteURI(url);
		request.ssl(reqOpt.isSsl());
		request.host(reqOpt.getHost());
		request.port(reqOpt.getPort());
		request.uri(reqOpt.getURI());
		request.method(task.getRequest().getMethod().toHttpMethod());
		// headers
		if (task.getRequest().getHeaders() != null) {
			task.getRequest().getHeaders().forEach((key, value) -> request.headers().set(key, value));
		}
		return url;
	}
	private String refreshVolatileRequestBody(MachinegunTask task) {
		return resolveString(task, task.getRequest().getBody());
	}

	private Handler<Message<JsonObject>> handleRequestEvent() {
		return msg_ -> {
			final MachinegunTask task = msg_.body().mapTo(MachinegunTask.class);
			logger.debug("Executing task #{}...", task.getIdx());
			vertx.executeBlocking(promise -> {
				try {
					String repeatsStr = resolveString(task, task.getRepeats());
					int repeats = repeatsStr.isEmpty() ? 1 : Integer.parseInt(repeatsStr);
					boolean volatileRequest = task.getRequest().getVolatile();
					// request
					HttpRequest<Buffer> request = client.request(task.getRequest().getMethod().toHttpMethod(), "/");
					// authorization
					Future<HttpRequest<Buffer>> authFuture;
					if (task.getAuthorization() != null) {
						authFuture = authorizeRequest(request, task);
					} else {
						authFuture = Future.succeededFuture(request);
					}
					// send
					authFuture
						.onSuccess(req_ -> {
							HttpRequest<Buffer> req = req_.copy();
							String url = refreshVolatileRequest(req, task);
							String body = refreshVolatileRequestBody(task);
							logger.debug("Sending {} request(s)...", repeats);
							List<Future<HttpResponse<Buffer>>> requestList = new ArrayList<>(repeats);
							long start = System.currentTimeMillis();
							for (int i = 0; i < repeats; i++) {
								if (i > 0 && task.getRequest().getVolatile()) {
									url = refreshVolatileRequest(req, task);
									body = refreshVolatileRequestBody(task);
								}
								if (logger.isTraceEnabled()) {
									logger.trace("Request details:\n\tMethod: {}\n\tURL: {}\n\tHeaders:\n{}\n\tBody: {}",
										task.getRequest().getMethod(),
										url,
										request.headers().entries().stream()
											.map(ent -> String.format("\t\t%s: %s", ent.getKey(), ent.getValue()))
											.collect(Collectors.joining("\n")),
										body);
								}
								Future<HttpResponse<Buffer>> responseFuture = (body == null || body.isEmpty())
									? req.send()
									: req.sendBuffer(Buffer.buffer(body));
								responseFuture
									.onSuccess(resp -> {
										if (logger.isTraceEnabled()) {
											logger.trace("Response status: {} {}\n\tHeaders:\n{}\n\tBody: {}",
												resp.statusCode(),
												resp.statusMessage(),
												resp.headers().entries().stream()
													.map(ent -> String.format("\t\t%s: %s", ent.getKey(), ent.getValue()))
													.collect(Collectors.joining("\n")),
												resp.bodyAsString());
										}
									})
									.onFailure(err -> {
										if (logger.isTraceEnabled()) {
											logger.trace("Response error: {}", err.getMessage(), err);
										}
									});
								requestList.add(responseFuture);
							}
							long sent = System.currentTimeMillis();
							logger.debug("Requests sent: {} ({} rps). Waiting responses...",
								repeats,
								Math.round(100000.0 * repeats / (sent - start)) / 100.0);
							CompositeFuture allResponses = CompositeFutureImpl.join(requestList.toArray(new Future[0]));
							allResponses.onComplete(allResp_ -> {
								long received = System.currentTimeMillis();
								CompositeFutureImpl allResp = (CompositeFutureImpl) allResp_;
								int respTotal = allResp.size();
								int respOk = 0;
								for (int i = 0; i < respTotal; i++) if (allResp.succeeded(i)) respOk++;
								logger.debug("Responses got: {} (succeeded: {}, failed: {}) ({} rps).",
									respTotal,
									respOk,
									respTotal - respOk,
									Math.round(100000.0 * repeats / (received - start)) / 100.0);
								promise.complete();
							});
						})
						.onFailure(authErr -> {
							logger.debug("Authorization error: {}", authErr.getMessage(), authErr);
							promise.fail(authErr);
						});
				} catch (Throwable err) {
					logger.error("Error handle task event: {}", err.getMessage(), err);
					promise.fail(err);
				}
			},
			res -> {
				// next message
				if (task.getId() != null) {
					if (vertx.sharedData().getLocalMap(MainVerticle.INTERVAL_MAP).containsKey(task.getId())) {
						// task is still scheduled
						sendTaskEvent(vertx, task, false);
					} else {
						logger.info("Interval task {} was cancelled. Terminating...", task.getId());
					}
				}
			});
		};
	}
}
