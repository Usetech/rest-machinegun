# Rest-machinegun

Utility for REST communication testing. Automatically sends series of specified requests.

## Configuration

Configuration is json file. Default paths is `conf/config.json`. This path can be overridden using the `vertx-config-path` system property or `VERTX_CONFIG_PATH` environment variable.

### Machinegun configuration

Here is the described configuration sample:

_!!! Note that in real json file **comments** are not allowed and should be removed after paste._

```json
{
  "machinegun": { /* machinegun config root */
    "tasks": [ // tasks with request definitions 
      {
        // interval between task executes, milliseconds (mustache*)
        // alternatively cron-expr can be used, see below
        "interval-ms": 10000,
        // environment variables (for use with mustache placeholder, for example: {{env.VAR_NAME}})
        "env": {
          "MAIL_FROM": "aaa", // (mustache*)
          "MAIL_TO": "customer_23@acme.com",
          "PASS": "secretWORD"
        },
        // specifies request
        "request": {
          // request target url (mustache*)
          "url": "http://localhost/api/mail/send-mail",
          // request method (one of GET, POST, PUT, DELETE, HEAD)
          "method": "POST",
          // request headers
          "headers": {
            "Content-Type": "application/json" // (mustache*)
          },
          // request body template (mustache*)
          "body": "{ \"phoneFrom\": \"{{env.PHONE_FROM}}\", \"phoneTo\": \"{{env.PHONE_TO}}\", \"message\": \"Message #{{counter.a}}\", \"smsTtlMs\": 0 }",
          // set true to recalculate request at every repeat
          "volatile": true
        },
        // optional autorization config (alternatively can be used OAuth2 or basic authorization)
        "authorization": {
          // OAuth2 specification (autorizes every time before task execution)
          "oauth2": { // all property valuest supports mustache placeholders
            "token-url": "http://auth:8080/realms/main/protocol/openid-connect/token",
            "client-id": "my-app",
            "client-secret": "9b527e11-8b83-4a5b-a81d-b0f505e882e1",
            "username": "user_{{random.range.1.4}}",
            "password": "{{env.PASS}}"
          }
        },
        // request count on every task execution
        "repeats": "{{random.range.4.20}}"
      },
      {
        // cron expression, specifies schedule for task executions
        "cron-expr": "17 0/1 * * * ? *",
        "request": {
          "url": "http://acme.com/counter?aaa={{counter.aaa}}",
          "method": "GET",
          "volatile": true
        },
        "authorization": {
          // basic authorization appends special header in requests
          "basic": {
            "username": "admin",
            "password": "1111"
          }
        },
        "repeats": 10
      }
    ]
  }
}
```

* Mustache is a logicless template engine for creating dynamic content like configuration files. The Mustache templates consist of tag names surrounded by `{{}}`. 

### Predefined placeholder expressions

- `env.<varuable>` - Value of environment variable with name followed by point (`.`).
Uses values described in task.env property, then system environment variables.

Examples: `{{env.DEBUG}}`, `{{env.USER_PASS}}`.
 
- `counter`, `counter.<name>` - Value of sequentally incrementing counter.
Can use any number of counters specified by unique names. If name is omitted then uses the _global_ counter.
Every counter starts with 1 and increments on every usage.
For use new counter values on every repeat use `task.volatole` property. 

Examples: `{{counter}}`, `{{counter.A}}`, `{{counter.count1}}`.

- `random.byte`, `random.int`, `random.integer`, `random.long` - Random value in range of specified type.
Byte range is `-128..127`; integer: `-2147483648..2147483647`; long: `-9223372036854775808..9223372036854775807`.
- `random.uuid` - String value with random UUID. Generates value like `"5ca436cc-d46a-47b2-8a2f-0ba3b85898ea"`.
- `random.range.<from>.<to>` - Random value in range `<from>..<to>`.
- `random.ascii.<lenght>` - Random ASCII character sequence of specified fixed length.
- `random.ascii.<min lenght>.<max length>` - Random ASCII character sequence. The length of the sequence is random value in specified range.
- `random.ascii.<min lenght>.<max length>.<regexp range>` - Random ASCII character sequence. The length of the sequence is random value in specified range. The last argument specifies the regular expression range to generate characters in order to match this range. Regexp range is inner content of `[]` statement (see example below).

Examples: `{{random.int}}` - a random value between -2147483648 and 2147483647 inclusive; `{{random.range.0.8}}` - a random value between 0 and 7 inclusive; `{{random.ascii.1024}}` - random string 1024 characters length (chars within codes from 32 to 255, ASCII); `{{random.ascii.16.16.A-Za-z0-9$%#!}}` - random string 16 of characters matching the regexp `[A-Za-z0-9$%#!]`.

## Running

To run utility from jar, use following command:

```shell
java -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory -Dlogback.configurationFile=config/logback.xml io.vertx.core.Launcher run com.usetech.rest_machinegun.MainVerticle
```

Use default config file name and location or use environment variable `VERTX_CONFIG_PATH` (see [Configuration](#configuration))
