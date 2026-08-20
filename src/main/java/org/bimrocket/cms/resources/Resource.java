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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.bimrocket.cms.ContentLoader;

/**
 *
 * @author realor
 */
public abstract class Resource
{
  public static final Map<String, Class<? extends Resource>> TYPE_CLASSES;

  static
  {
    TYPE_CLASSES = new HashMap<>();
    TYPE_CLASSES.put("peb", HtmlResource.class);
    TYPE_CLASSES.put("njk", HtmlResource.class);
    TYPE_CLASSES.put("htm", HtmlResource.class);
    TYPE_CLASSES.put("html", HtmlResource.class);
    TYPE_CLASSES.put("md", MarkdownResource.class);
    TYPE_CLASSES.put("yml", YamlResource.class);
    TYPE_CLASSES.put("yaml", YamlResource.class);
  }

  public static Resource create(Site site, String pathName)
  {
    int index = pathName.lastIndexOf('.');
    if (index == -1)
      throw new RuntimeException("Invalid resourceName: " + pathName);

    String type = pathName.substring(index + 1).toLowerCase();

    try
    {
      var cls = TYPE_CLASSES.get(type);
      if (cls == null) cls = AssetResource.class;
      var constructor = cls.getConstructor(Site.class, String.class);
      return constructor.newInstance(site, pathName);
    }
    catch (Exception ex)
    {
      throw new RuntimeException(ex);
    }
  }

  protected Site site;
  protected String pathName;
  protected long lastModified;
  protected long size;

  public Resource(Site site, String pathName)
  {
    this.site = site;
    this.pathName = pathName;
  }

  public Site getSite()
  {
    return site;
  }

  public String getPathName()
  {
    return pathName;
  }

  public String getFileName()
  {
    int index = pathName.lastIndexOf('/');
    return index == -1 ? pathName : pathName.substring(index + 1);
  }

  public String getFileType()
  {
    int index = pathName.lastIndexOf('.');
    return index == -1 ? "" : pathName.substring(index + 1);
  }

  public String getSimpleName()
  {
    String fileName = getFileName();
    int index = fileName.lastIndexOf('.');
    return index == -1 ? fileName : fileName.substring(0, index);
  }

  public String getSimplePathName()
  {
    int index = pathName.lastIndexOf('.');
    return index == -1 ? pathName : pathName.substring(0, index);
  }

  public long getLastModified()
  {
    return lastModified;
  }

  public Date getLastModifiedDate()
  {
    return new Date(lastModified);
  }

  public long getSize()
  {
    return size;
  }

  public String getSizeText()
  {
    String[] units = { "B", "KB", "MB", "GB", "TB" };
    double value = size;
    int unit = 0;

    while (value >= 1024 && unit < units.length - 1)
    {
      value /= 1024;
      unit++;
    }

    return unit == 0 ?
      String.format("%d %s", size, units[unit]) :
      String.format("%.1f %s", value, units[unit]);
  }

  public String getParentPathName()
  {
    int index = pathName.lastIndexOf('/');
    if (index == -1) return "";
    return pathName.substring(0, index);
  }

  public <T> T getProperty(String name)
  {
    return (T)getProperties().get(name);
  }

  public Map<String, Object> getProperties()
  {
    return Collections.emptyMap();
  }

  public boolean exists()
  {
    return site.getLoader().resourceExists(pathName);
  }

  public void load() throws IOException
  {
    ContentLoader loader = site.getLoader();
    long newLastModified = loader.getLastModified(pathName);

    if (lastModified != newLastModified)
    {
      read();
      lastModified = newLastModified;
      size = loader.getSize(pathName);
    }
  }

  public abstract void send(HttpServletRequest req, HttpServletResponse resp)
    throws IOException;

  protected void read() throws IOException
  {
  }
}
