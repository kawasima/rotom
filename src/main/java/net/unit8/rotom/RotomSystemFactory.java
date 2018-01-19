package net.unit8.rotom;

import enkan.Env;
import enkan.component.ApplicationComponent;
import enkan.component.builtin.HmacEncoder;
import enkan.component.freemarker.FreemarkerTemplateEngine;
import enkan.component.jackson.JacksonBeansConverter;
import enkan.component.jetty.JettyComponent;
import enkan.component.metrics.MetricsComponent;
import enkan.config.EnkanSystemFactory;
import enkan.system.EnkanSystem;
import net.unit8.bouncr.sign.JsonWebToken;
import net.unit8.rotom.model.Wiki;
import net.unit8.rotom.search.IndexManager;

import java.nio.file.Paths;

import static enkan.component.ComponentRelationship.component;
import static enkan.util.BeanBuilder.builder;

public class RotomSystemFactory implements EnkanSystemFactory {
    @Override
    public EnkanSystem create() {
        return EnkanSystem.of(
                "hmac", new HmacEncoder(),
                "jackson", new JacksonBeansConverter(),
                "template", new FreemarkerTemplateEngine(),
                "metrics", new MetricsComponent(),
                "wiki", builder(new Wiki())
                        .set(Wiki::setRepositoryPath, Paths.get("wiki"))
                        .build(),
                "index", builder(new IndexManager())
                        .set(IndexManager::setIndexPath, Paths.get("index"))
                        .build(),
                "config", builder(new RotomConfiguration())
                        .set(RotomConfiguration::setBasePath, "/wiki")
                        .build(),
                "jwt", new JsonWebToken(),
                "app", new ApplicationComponent("net.unit8.rotom.RotomApplicationFactory"),
                "http", builder(new JettyComponent())
                        .set(JettyComponent::setPort, Env.getInt("PORT", 3000))
                        .build()
        ).relationships(
                component("http").using("app"),
                component("app").using(
                        "config", "template", "jackson", "metrics", "jwt", "wiki", "index")
        );
    }
}
