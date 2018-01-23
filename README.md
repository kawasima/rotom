Rotom
=======

Rotom is a git based Wiki. It's gollum clone.

## Get started

```shell
% mvn package
% java -jar target/rotom-0.1.0-SNAPSHOT.jar
```

or

[![Heroku Deploy](https://www.herokucdn.com/deploy/button.png)](https://heroku.com/deploy?template=https://github.com/kawasima/rotom)

## Authentication

Rotom supports [bouncr](https://github.com/kawasima/bouncr) authentication and authorization.

|Permission  |Operation|
|:---------  |:--------|
|page:read   |Show a page and it's history and search pages|
|page:create |Create a new page|
|page:edit   |Edit a page  |
|page:delete |Delete a page|

When you use BouncrBackend, bouncr support is enabled.

```java
RotomConfiguration configuration = new RotomConfiguration();
configuration.setAuthBackend(new BouncrBackend());
```
