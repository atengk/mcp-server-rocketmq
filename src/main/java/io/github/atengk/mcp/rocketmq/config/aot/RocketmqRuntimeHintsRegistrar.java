package io.github.atengk.mcp.rocketmq.config.aot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * RocketMQ 客户端 AOT 运行时元数据提示注册器。
 * 针对 Apache RocketMQ 遗留 Remoting 协议（rocketmq-tools 5.3.1）中大量动态反射与自定义 Header 编解码，
 * 显式注册 GraalVM Native Image 所需的反射白名单与资源元数据。
 *
 * @author Ateng
 * @since 2026-10-05
 */
public class RocketmqRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

    private static final Logger log = LoggerFactory.getLogger(RocketmqRuntimeHintsRegistrar.class);

    private static final String[] SCAN_PACKAGES = {
            "org/apache/rocketmq/remoting/protocol",
            "org/apache/rocketmq/common/message",
            "io/github/atengk/mcp/rocketmq/rocketmq/admin/dto",
            "io/github/atengk/mcp/rocketmq/rocketmq/messaging/dto"
    };

    private static final Class<?>[] EXPLICIT_CLASSES = {
            org.apache.rocketmq.common.TopicConfig.class,
            org.apache.rocketmq.common.TopicFilterType.class,
            org.apache.rocketmq.common.constant.PermName.class,
            org.apache.rocketmq.common.consumer.ConsumeFromWhere.class,
            org.apache.rocketmq.acl.common.AclClientRPCHook.class,
            org.apache.rocketmq.acl.common.SessionCredentials.class,
            io.github.atengk.mcp.rocketmq.config.RocketmqProperties.class,
            com.alibaba.fastjson.JSONObject.class,
            com.alibaba.fastjson.JSONArray.class,
            com.alibaba.fastjson.parser.ParserConfig.class
    };

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        ClassLoader targetLoader = classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(targetLoader);

        Set<String> classNames = new LinkedHashSet<>();

        // 1. 批量扫描包路径下的所有协议类
        for (String pkg : SCAN_PACKAGES) {
            String locationPattern = "classpath*:" + pkg + "/**/*.class";
            try {
                Resource[] resources = resolver.getResources(locationPattern);
                for (Resource resource : resources) {
                    String uri = resource.getURI().toString();
                    int idx = uri.indexOf(pkg);
                    if (idx != -1) {
                        String relativePath = uri.substring(idx);
                        if (relativePath.endsWith(".class") && !relativePath.contains("$")) {
                            String className = relativePath
                                    .replace('/', '.')
                                    .replace('\\', '.')
                                    .substring(0, relativePath.length() - ".class".length());
                            classNames.add(className);
                        }
                    }
                }
            } catch (IOException e) {
                log.warn("AOT 扫描包路径 [{}] 失败: {}", pkg, e.getMessage());
            }
        }

        // 2. 注册批量扫描发现的类
        for (String className : classNames) {
            try {
                Class<?> clazz = ClassUtils.forName(className, targetLoader);
                registerType(hints, clazz);
            } catch (Throwable ignored) {
                // 部分依赖类可能在类加载器中不完全匹配，保持安全忽略
            }
        }

        // 3. 注册关键防御性显式类
        for (Class<?> clazz : EXPLICIT_CLASSES) {
            registerType(hints, clazz);
        }

        // 4. 注册 Spring Boot 核心工厂类、推断器与上下文初始化器（防范 Native 运行期反射剪裁缺失）
        String[] springBootCoreClasses = {
                "org.springframework.boot.webmvc.WebMvcWebApplicationTypeDeducer",
                "org.springframework.boot.webflux.WebFluxWebApplicationTypeDeducer",
                "org.springframework.boot.webmvc.autoconfigure.JspTemplateAvailabilityProvider",
                "org.springframework.boot.web.server.context.ServerPortInfoApplicationContextInitializer",
                "org.springframework.boot.web.server.context.WebServerInitializedEvent",
                "org.springframework.boot.web.server.context.MissingWebServerFactoryBeanFailureAnalyzer",
                "org.springframework.boot.web.server.PortInUseFailureAnalyzer",
                "org.springframework.boot.web.server.reactive.context.ReactiveWebServerApplicationContextFactory",
                "org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContextFactory",
                "org.springframework.boot.web.context.reactive.FilteredReactiveWebContextResourceFilePathResolver",
                "org.springframework.boot.web.context.servlet.ServletContextResourceFilePathResolver",
                "org.springframework.boot.context.ConfigurationWarningsApplicationContextInitializer",
                "org.springframework.boot.context.ContextIdApplicationContextInitializer",
                "org.springframework.boot.io.ProtocolResolverApplicationContextInitializer",
                "org.springframework.boot.ClearCachesApplicationListener",
                "org.springframework.boot.builder.ParentContextCloserApplicationListener",
                "org.springframework.boot.context.FileEncodingApplicationListener",
                "org.springframework.boot.context.logging.LoggingApplicationListener",
                "org.springframework.boot.support.AnsiOutputApplicationListener",
                "org.springframework.boot.support.EnvironmentPostProcessorApplicationListener",
                "org.springframework.boot.autoconfigure.SharedMetadataReaderFactoryContextInitializer",
                "org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener",
                "org.springframework.boot.autoconfigure.preinitialize.BackgroundPreinitializingApplicationListener",
                "org.springframework.boot.context.event.EventPublishingRunListener",
                "org.springframework.boot.logging.java.JavaLoggingSystem$Factory",
                "org.springframework.boot.logging.java.JavaLoggingSystem",
                "org.springframework.boot.logging.log4j2.Log4J2LoggingSystem$Factory",
                "org.springframework.boot.logging.log4j2.Log4J2LoggingSystem",
                "org.springframework.boot.logging.logback.LogbackLoggingSystem$Factory",
                "org.springframework.boot.logging.logback.LogbackLoggingSystem",
                "org.springframework.boot.io.ApplicationResourceLoader$FilePathResolver"
        };
        for (String className : springBootCoreClasses) {
            try {
                Class<?> clazz = ClassUtils.forName(className, targetLoader);
                registerType(hints, clazz);
            } catch (Throwable ignored) {
            }
        }
    }

    private void registerType(RuntimeHints hints, Class<?> clazz) {
        hints.reflection().registerType(clazz,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS,
                MemberCategory.DECLARED_FIELDS
        );
    }
}
