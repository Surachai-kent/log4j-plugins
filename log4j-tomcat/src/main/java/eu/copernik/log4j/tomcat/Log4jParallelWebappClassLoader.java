/*
 * Copyright © 2023 Piotr P. Karwasz
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

import org.apache.catalina.LifecycleException;
import org.apache.catalina.loader.ParallelWebappClassLoader;

/**
 * A classloader that delegates lookups for Log4j2 API classes to the parent classloader.
 *
 * <p>This allows multiple applications bundled with Log4j2 API to use a single copy of Log4j2 in
 * the parent classloader, without modifying the servlet delegation model for the application.
 *
 * <p>In order to replace the default application classloader add:
 *
 * <pre>
 * &lt;Context&gt;
 *     ...
 *     &lt;Loader loaderClass="eu.copernik.log4j.tomcat.Log4jParallelWebappClassLoader"/&gt;
 * &lt;/Context&gt;
 * </pre>
 *
 * <p>to the application's context definition (cf. <a href=
 * "https://tomcat.apache.org/tomcat-10.0-doc/config/context.html#Defining_a_context">defining a
 * context</a>).
 */
public class Log4jParallelWebappClassLoader extends ParallelWebappClassLoader {

    public Log4jParallelWebappClassLoader() {
        super();
    }

    public Log4jParallelWebappClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    protected boolean filter(String name, boolean isClassName) {
        if (name == null || name.length() < 25) {
            return super.filter(name, isClassName);
        }
        if (ClassLoaderUtil.isLog4jApiResource(name, isClassName)) {
            return true;
        }
        return super.filter(name, isClassName);
    }

    @Override
    public Log4jParallelWebappClassLoader copyWithoutTransformers() {

        Log4jParallelWebappClassLoader result = new Log4jParallelWebappClassLoader(getParent());

        super.copyStateWithoutTransformers(result);

        try {
            result.start();
        } catch (LifecycleException e) {
            throw new IllegalStateException(e);
        }

        return result;
    }
}
