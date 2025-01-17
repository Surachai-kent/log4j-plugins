/*
 * Copyright © 2022 Piotr P. Karwasz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.copernik.log4j.tomcat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.WebResourceRoot.ResourceSetType;
import org.apache.catalina.loader.WebappClassLoader;
import org.apache.catalina.loader.WebappClassLoaderBase;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.util.IOUtils;
import org.apache.logging.log4j.internal.DefaultLogBuilder;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.simple.SimpleLogger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.assertj.core.api.ClassAssert;
import org.junit.jupiter.api.MethodOrderer.Random;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.provider.Arguments;

@TestMethodOrder(Random.class)
public class Log4jWebappClassLoaderTest {

    private static final String LOG4J_API_LINK = "org.apache.logging.log4j.api.link";
    private static final String LOG4J_CORE_LINK = "org.apache.logging.log4j.core.link";

    private URL findJar(final String link) throws IOException {
        final ClassLoader cl = Log4jWebappClassLoaderTest.class.getClassLoader();
        try (final InputStream is = cl.getResourceAsStream(link);
                final Reader reader = new InputStreamReader(is, StandardCharsets.US_ASCII)) {
            return new URL(IOUtils.toString(reader));
        }
    }

    private <T extends WebappClassLoaderBase> T createClassLoader(final Class<T> clazz) {
        try {
            final WebResourceRoot resources = new StandardRoot(mock(Context.class));
            resources.createWebResourceSet(
                    ResourceSetType.CLASSES_JAR, "/WEB-INF/classes", findJar(LOG4J_API_LINK), "/");
            resources.createWebResourceSet(
                    ResourceSetType.CLASSES_JAR, "/WEB-INF/classes", findJar(LOG4J_CORE_LINK), "/");
            resources.start();
            final Constructor<T> constructor = clazz.getConstructor(ClassLoader.class);
            final T cl = constructor.newInstance(Log4jWebappClassLoaderTest.class.getClassLoader());
            cl.setResources(resources);
            cl.start();
            return cl;
        } catch (ReflectiveOperationException | LifecycleException | IOException e) {
            fail("Unable to instantiate classloader.", e);
        }
        // never reached
        return null;
    }

    static Stream<Arguments> classes() {
        return Stream.of(
                Arguments.of(LogManager.class, true, false),
                Arguments.of(DefaultLogBuilder.class, true, false),
                Arguments.of(Message.class, true, false),
                Arguments.of(SimpleLogger.class, true, false),
                Arguments.of(LoggerContext.class, true, false),
                Arguments.of(StatusListener.class, true, false),
                Arguments.of(PropertiesUtil.class, true, false),
                // TODO: loading LoggerContext fails
                // Arguments.of(org.apache.logging.log4j.core.LoggerContext.class, false,
                // false),
                Arguments.of(Configuration.class, false, false));
        // Arguments.of(TomcatLookup.class, true, true));
    }

    @RepeatedTest(100)
    public void testStandardClassloader() throws IOException {
        try (final URLClassLoader cl = createClassLoader(WebappClassLoader.class); ) {
            classes().forEach(arg -> {
                final Class<?> clazz = (Class<?>) arg.get()[0];
                final boolean isEqual = (boolean) arg.get()[2];
                final Class<?> otherClazz = assertDoesNotThrow(() -> Class.forName(clazz.getName(), true, cl));
                final ClassAssert assertion = assertThat(otherClazz);
                if (isEqual) {
                    assertion.isEqualTo(clazz);
                } else {
                    assertion.isNotEqualTo(clazz);
                }
            });
        }
    }

    @RepeatedTest(100)
    public void testLog4jClassloader() throws IOException {
        try (final URLClassLoader cl = createClassLoader(Log4jWebappClassLoader.class); ) {
            classes().forEach(arg -> {
                final Class<?> clazz = (Class<?>) arg.get()[0];
                final boolean isEqual = (boolean) arg.get()[1];
                final Class<?> otherClazz = assertDoesNotThrow(() -> Class.forName(clazz.getName(), true, cl));
                final ClassAssert assertion = assertThat(otherClazz);
                if (isEqual) {
                    assertion.isEqualTo(clazz);
                } else {
                    assertion.isNotEqualTo(clazz);
                }
            });
        }
    }

    @RepeatedTest(100)
    public void testParallelLog4jClassloader() throws IOException {
        try (final URLClassLoader cl = createClassLoader(Log4jParallelWebappClassLoader.class); ) {
            classes().forEach(arg -> {
                final Class<?> clazz = (Class<?>) arg.get()[0];
                final boolean isEqual = (boolean) arg.get()[1];
                final Class<?> otherClazz = assertDoesNotThrow(() -> Class.forName(clazz.getName(), true, cl));
                final ClassAssert assertion = assertThat(otherClazz);
                if (isEqual) {
                    assertion.isEqualTo(clazz);
                } else {
                    assertion.isNotEqualTo(clazz);
                }
            });
        }
    }
}
