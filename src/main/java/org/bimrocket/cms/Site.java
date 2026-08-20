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
package org.bimrocket.cms;

import org.bimrocket.cms.resources.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import org.apache.commons.collections4.map.LRUMap;
import org.bimrocket.cms.resources.DataResource;

/**
 *
 * @author realor
 */
public class Site
{
  final String name;
  final SiteConfig config;
  final ContentLoader loader;

  final Map<String, Resource> resourceCache;
  final Map<String, Object> internalData = new ConcurrentHashMap<>();

  public Site(String name, String baseDir, SiteConfig config)
  {
    this(name, baseDir, config, 100);
  }

  public Site(String name, String baseDir, SiteConfig config, int size)
  {
    this.name = name;
    this.config = config;
    this.loader = new ContentLoader(baseDir);
    resourceCache = Collections.synchronizedMap(new LRUMap<>(size));
  }

  public String getName()
  {
    return name;
  }

  public SiteConfig getConfig()
  {
    return config;
  }

  public ContentLoader getLoader()
  {
    return loader;
  }

  public Map<String, Object> getInternalData()
  {
    return internalData;
  }

  public Map<String, Object> getData() throws IOException
  {
    Map<String, Object> data = new HashMap<>();
    String dataPathName = config.getDataPathName();
    List<String> childNames = loader.getChildNames(dataPathName);
    for (String childName : childNames)
    {
      Resource resource = getResourceFromCache(dataPathName + "/" + childName);
      if (resource instanceof DataResource dataResource)
      {
        data.put(resource.getSimpleName(), dataResource.getData());
      }
    }
    return data;
  }

  public Resource getResource(String pathName)
  {
    return getResource(pathName, config.getTemplateTypes());
  }

  public Resource getResource(String pathName, String[] fileTypes)
  {
    Resource resource;

    int index = pathName.lastIndexOf('/');
    boolean haveType = pathName.lastIndexOf('.') > index;

    if (haveType)
    {
      resource = findResource(pathName);
    }
    else
    {
      resource = null;

      int i = 0;
      while (i < fileTypes.length && resource == null)
      {
        resource = findResource(pathName + "." + fileTypes[i]);
        i++;
      }
    }
    return resource;
  }

  public List<Resource> getResources(String pathName) throws IOException
  {
    return getResources(pathName, null);
  }

  public List<Resource> getResources(String pathName, String pattern)
    throws IOException
  {
    return getResources(pathName, pattern, null);
  }

  public List<Resource> getResources(
    String pathName, String pattern, String orderBy) throws IOException
  {
    Predicate<String> predicate = pattern == null ?
      p -> true : p -> p.matches(pattern);

    List<String> names = loader.getChildNames(pathName, predicate);
    List<Resource> resources = new ArrayList<>();
    for (String name : names)
    {
      Resource resource = getResourceFromCache(pathName + "/" + name);
      resource.load();
      resources.add(resource);
    }

    if (orderBy != null)
    {
      int index = orderBy.indexOf(":");
      String property = index > 0 ? orderBy.substring(0, index) : orderBy;
      boolean descending = orderBy.endsWith(":desc");

      Collections.sort(resources, (r1, r2) ->
      {
        Comparable c1 = r1.getProperty(property);
        Comparable c2 = r2.getProperty(property);

        if (c1 == null && c2 == null) return 0;
        if (c1 == null && c2 != null) return descending ? 1 : -1;
        if (c1 != null && c2 == null) return descending ? -1 : 1;

        return descending ? c2.compareTo(c1) : c1.compareTo(c2);
      });
    }
    return resources;
  }

  Resource findResource(String pathName)
  {
    if (!loader.resourceExists(pathName)) return null;

    return getResourceFromCache(pathName);
  }

  Resource getResourceFromCache(String pathName)
  {
    Resource resource = resourceCache.get(pathName);
    if (resource == null)
    {
      resource = Resource.create(this, pathName);
      resourceCache.put(pathName, resource);
    }
    return resource;
  }
}
