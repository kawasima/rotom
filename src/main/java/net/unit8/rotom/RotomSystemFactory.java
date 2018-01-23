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
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;

import static enkan.component.ComponentRelationship.component;
import static enkan.util.BeanBuilder.builder;

public class RotomSystemFactory implements EnkanSystemFactory {
    public Repository getRepository() {
        try {
            Repository repository = FileRepositoryBuilder.create(new File(Env.getString("REPO_PATH", "wiki")));
            if (!repository.getDirectory().exists()) {
                repository.create(true);
            }
            return repository;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public EnkanSystem create() {
        return EnkanSystem.of(
                "hmac", new HmacEncoder(),
                "jackson", new JacksonBeansConverter(),
                "template", new FreemarkerTemplateEngine(),
                "metrics", new MetricsComponent(),
                "wiki", builder(new Wiki())
                        .set(Wiki::setRepository, getRepository())
                        .build(),
                "index", builder(new IndexManager())
                        .set(IndexManager::setIndexPath, Paths.get("index"))
                        .build(),
                "config", builder(new RotomConfiguration())
                        .set(RotomConfiguration::setBasePath, Env.getString("BASE_PATH", ""))
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
