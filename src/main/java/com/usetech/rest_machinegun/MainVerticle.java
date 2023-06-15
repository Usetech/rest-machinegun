package com.usetech.rest_machinegun;

import com.diabolicallabs.vertx.cron.CronEventSchedulerVertical;
import com.usetech.rest_machinegun.config.MachinegunConfig;
import com.usetech.rest_machinegun.config.MachinegunTask;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainVerticle extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

	@Override
	public void start(Promise<Void> startPromise) {
		// config
		ConfigRetriever configRetriever = ConfigRetriever.create(vertx);
		configRetriever.getConfig()
			.onSuccess(config -> {
				logger.info("Configuration retrieved. Deploying vertices...");
				deployVertices(config);
			});

		// ... reloading
		configRetriever.listen(change -> {
			logger.info("Configuration has been changed. Redeploying vertices...");
			undeployVertices();
			deployVertices(change.getNewConfiguration());
		});

	}

	static final String DEPLOYMENT_MAP = "deployment";
	static final String CRON_MAP = "crons";
	static final String INTERVAL_MAP = "intervals";
	static final String CRON_ADDRESS = "cron_address";

	@SuppressWarnings("rawtypes")
	private void undeployVertices() {
		// cancel crons
		LocalMap<String, Object> cronMap = vertx.sharedData().getLocalMap(CRON_MAP);
		List<Future> cronCancelFutures = new ArrayList<>(cronMap.size());
		logger.info("Cancelling cron schedules (count: {})...", cronMap.size());
		cronMap.keySet()
			.forEach(cronId -> cronCancelFutures.add(vertx.eventBus().request(CRON_ADDRESS + ".cancel", cronId)));
		CompositeFuture allCrons = CompositeFuture.all(cronCancelFutures);
		allCrons
			.onSuccess(cf -> logger.info("Cron schedules are successfully cancelled (count: {}).", cf.list().size()))
			.onFailure(ce -> logger.error("Error canceling cron schedule: {}" + ce.getMessage(), ce));
		// unregister intervals
		LocalMap<String, Object> intervalMap = vertx.sharedData().getLocalMap(INTERVAL_MAP);
		logger.info("Cancelling interval schedules (count: {})...", intervalMap.size());
		intervalMap.clear();
		logger.info("All interval schedules are cancelled.");
		// undeploy vertices
		LocalMap<String, Object> deplMap = vertx.sharedData().getLocalMap(DEPLOYMENT_MAP);
		logger.info("Undeploying vertices (count: {})...", deplMap.size());
		List<Future> undeplFutures = new ArrayList<>(deplMap.size());
		deplMap.keySet()
			.forEach(depId -> undeplFutures.add(vertx.undeploy(depId)));
		CompositeFuture all = CompositeFuture.all(undeplFutures);
		all
			.onSuccess(cf -> logger.info("Vertices are successfully undeployed (count: {}).", cf.list().size()))
			.onFailure(ce -> logger.error("Error undeploying old vertices: {}" + ce.getMessage(), ce));
	}

	private void deployVertices(JsonObject config) {
		JsonObject mgConfigJson = config.getJsonObject("machinegun");
		if (mgConfigJson != null) {
			MachinegunConfig mgConfig = mgConfigJson.mapTo(MachinegunConfig.class);
			List<MachinegunTask> intervalTasks = mgConfig.getTasks().stream().filter(t -> t.getIntervalMs() != null).toList();
			List<MachinegunTask> cronTasks = mgConfig.getTasks().stream().filter(t -> t.getIntervalMs() == null && t.getCronExpr() != null).toList();
			if (cronTasks.size() > 0) {
				logger.info("Deploying vertices (count: {})...", cronTasks.size());
				vertx.deployVerticle(CronEventSchedulerVertical.class,
					new DeploymentOptions()
						.setConfig(new JsonObject().put("address_base", CRON_ADDRESS))
				);
			}
			// Deploy vertices
			vertx.deployVerticle(
				MachinegunVerticle.class,
				new DeploymentOptions()
					.setWorker(true)
					.setWorkerPoolSize(mgConfig.getVertices() != null ? mgConfig.getVertices() : 8)
			)
				.onSuccess(depId -> {
					vertx.sharedData().getLocalMap(DEPLOYMENT_MAP).put(depId, depId);
					logger.info("{} successfully deployed", MachinegunVerticle.class.getSimpleName());
					// Send interval events
					for (MachinegunTask task : intervalTasks) {
						task.setId(UUID.randomUUID().toString()); // generate task id
						vertx.sharedData().getLocalMap(INTERVAL_MAP).put(task.getId(), "1");
						MachinegunVerticle.sendTaskEvent(vertx, task, true);
					}
				});
			// Send cron events
			LocalMap<Object, Object> cronMap = vertx.sharedData().getLocalMap(CRON_MAP);
			for (MachinegunTask task : cronTasks) {
				vertx.eventBus().request(
					CRON_ADDRESS + ".schedule",
					new JsonObject()
						.put("cron_expression", task.getCronExpr())
						.put("address", MachinegunVerticle.MACHINEGUN_REQUEST_EVENT_ADDRESS)
						.put("message", JsonObject.mapFrom(task)),
					reply -> {
						if (reply.succeeded()) {
							String cronId = (String) reply.result().body();
							cronMap.put(cronId, cronId);
							logger.info("Cron schedule registered (id: {})", cronId);
						} else {
							logger.error("Error registering task cron schedule ({}): {}", task.getCronExpr(), reply.cause().getMessage(), reply.cause());
						}
					}
				);
			}
		}
	}
}
