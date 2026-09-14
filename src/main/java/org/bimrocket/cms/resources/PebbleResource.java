/*
 * BIMROCKET
 *
 * Copyright (C) 2021-2026, Ajuntament de Sant Feliu de Llobregat
 *
 * This program is licensed and may be used, modified and redistributed under
 * the terms of the European Public License (EUPL), either version 1.1 or (at
 * your option) any later version as soon as they are approved by the European
 * Commission.
 *
 * Alternatively, you may redistribute and/or modify this program under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either  version 3 of the License, or (at your option)
 * any later version.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the licenses for the specific language governing permissions, limitations
 * and more details.
 *
 * You should have received a copy of the EUPL1.1 and the LGPLv3 licenses along
 * with this program; if not, you may find them at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 * http://www.gnu.org/licenses/
 * and
 * https://www.gnu.org/licenses/lgpl.txt
 */
package org.bimrocket.cms.resources;

import org.bimrocket.cms.Site;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.bimrocket.cms.SiteConfig;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author realor
 */
public abstract class PebbleResource extends Resource
{
  protected static final String ENGINE = "PebbleEngine";
  protected static final String LAYOUT = "layout";
  protected static final String CONTENT = "content";

  protected Map<String, Object> properties;
  protected PebbleTemplate template;

  public PebbleResource(Site site, String pathName)
  {
    super(site, pathName);
  }

  @Override
  public Map<String, Object> getProperties()
  {
    if (properties == null) return Collections.emptyMap();

    return properties;
  }

  @Override
  public void send(HttpServletRequest req, HttpServletResponse resp)
    throws IOException
  {
    load();

    resp.setContentType("text/html");
    StringWriter writer = new StringWriter();
    var context = createContext(req);
    template.evaluate(writer, context);
    String content = transformContent(writer.toString());
    String layout = getProperty(LAYOUT);
    if (layout != null)
    {
      context.put(CONTENT, content);
      SiteConfig config = site.getConfig();
      String layoutPathName = config.getIncludesPathName() + "/" + layout;
      PebbleTemplate layoutTemplate = getEngine().getTemplate(layoutPathName);
      writer = new StringWriter();
      layoutTemplate.evaluate(writer, context);
      content = writer.toString();
    }
    resp.getWriter().println(content);
  }

  protected String transformContent(String content)
  {
    return content;
  }

  @Override
  protected void read() throws IOException
  {
    System.out.println("Reader resource " + pathName);
    String source = site.getLoader().getResourceAsString(pathName);

    if (source.startsWith("---"))
    {
      int index = source.indexOf("\n", 3);
      if (index != -1)
      {
        int start = index + 1;

        int end = source.indexOf("\n---", start);

        if (end != -1)
        {
          index = source.indexOf("\n", end + 4);
          if (index != -1)
          {
            String yamlText = source.substring(start, end);
            source = source.substring(index).stripLeading();

            Yaml yaml = new Yaml();
            properties = yaml.load(yamlText);
          }
        }
      }
    }
    else
    {
      properties = null;
    }

    template = getEngine().getLiteralTemplate(source);
  }

  protected Map<String, Object> createContext(HttpServletRequest req)
    throws IOException
  {
    var context = new HashMap<String, Object>();

    Map<String, String[]> parameterMap = req.getParameterMap();
    if (!parameterMap.isEmpty())
    {
      context.put("parameters", parameterMap);
    }

    if (properties != null)
    {
      context.putAll(properties);
    }

    boolean isWebmaster = req.getSession().getAttribute("webmaster") != null;

    context.put("contextPath", req.getContextPath());
    context.put("site", site);
    context.put("resource", this);
    context.put("data", site.getData());
    context.put("webmaster", isWebmaster);

    return context;
  }

  protected PebbleEngine getEngine()
  {
    var data = site.getInternalData();
    PebbleEngine engine = (PebbleEngine)data.get(ENGINE);
    if (engine == null)
    {
      engine = new PebbleEngine.Builder()
      .loader(site.getLoader())
      .cacheActive(false)
      .build();
      data.put(ENGINE, engine);
    }
    return engine;
  }
}
