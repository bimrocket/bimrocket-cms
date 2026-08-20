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
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import static jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * @author realor
 */
@WebServlet("/*")
public class ResourceServlet extends HttpServlet
{
  public static final String BASE_DIR = "baseDir";
  public static final String INCLUDES_PATH_NAME = "includesPathName";
  public static final String DATA_PATH_NAME = "dataPathName";
  public static final String RESOURCE_CACHE_SIZE = "resourceCacheSize";

  public static final String PRO_SITE = "pro";
  public static final String DEV_SITE = "dev";

  public static final int DEFAULT_RESOURCE_CACHE_SIZE = 1000;

  Site proSite;
  Site devSite;

  @Override
  public void init(ServletConfig servletConfig) throws ServletException
  {
    ServletContext servletContext = servletConfig.getServletContext();
    String baseDir = System.getProperty("bimrocket.cms." + BASE_DIR);
    if (baseDir == null)
    {
      baseDir = servletContext.getInitParameter(BASE_DIR);
      if (baseDir == null)
      {
        baseDir = getDefaultBaseDir();
      }
    }

    try
    {
      String sizeText = servletContext.getInitParameter(RESOURCE_CACHE_SIZE);
      int size = sizeText == null ?
        DEFAULT_RESOURCE_CACHE_SIZE : Integer.parseInt(sizeText);

      SiteConfig config  = new SiteConfig();

      String includesPathName = servletContext.getInitParameter(INCLUDES_PATH_NAME);
      if (includesPathName != null)
      {
        config.setIncludesPathName(includesPathName);
      }

      String dataPathName = servletContext.getInitParameter(DATA_PATH_NAME);
      if (dataPathName != null)
      {
        config.setDataPathName(dataPathName);
      }

      servletContext.log("CMS baseDir: %s".formatted(baseDir));

      String proBaseDir = baseDir + "/" + PRO_SITE;
      String devBaseDir = baseDir + "/" + DEV_SITE;

      Files.createDirectories(Path.of(proBaseDir));
      Files.createDirectories(Path.of(devBaseDir));

      proSite = new Site(PRO_SITE, proBaseDir, config, size);
      servletContext.log(
        "PRO site created in %s".formatted(baseDir + PRO_SITE));

      devSite = new Site(DEV_SITE, devBaseDir, config, size);
      servletContext.log(
        "DEV site created in %s".formatted(baseDir + DEV_SITE));
    }
    catch (Exception ex)
    {
      throw new ServletException(ex);
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException
  {
    String siteName = (String)req.getParameter("site");
    if (siteName != null)
    {
      req.getSession().setAttribute("site", siteName);
    }

    String resourcePathName = getResourcePathName(req);

    if (resourcePathName.startsWith("_"))
    {
      resp.sendError(SC_NOT_FOUND);
      return;
    }

    Site site = getSite(req);

    if (resourcePathName.length() == 0)
    {
      resp.sendRedirect(req.getContextPath() + "/" +
        site.config.getDefaultFileName());
      return;
    }

    Resource resource = site.getResource(resourcePathName);

    if (resource == null)
    {
      resp.sendError(SC_NOT_FOUND);
      return;
    }

    resource.send(req, resp);
  }

  Site getSite(HttpServletRequest req)
  {
    String siteName = (String)req.getSession().getAttribute("site");
    if ("dev".equals(siteName))
    {
      return devSite;
    }
    else
    {
      return proSite;
    }
  }

  String getResourcePathName(HttpServletRequest req)
  {
    String uri = req.getRequestURI();
    String contextPath = req.getContextPath();

    String pathName = uri.substring(contextPath.length());
    if (pathName.startsWith("/"))
    {
      pathName = pathName.substring(1);
    }

    if (pathName.endsWith("/"))
    {
      pathName = pathName.substring(0, pathName.length() - 1);
    }
    return pathName;
  }

  String getDefaultBaseDir()
  {
    return System.getProperty("user.home") + "/bimrocket/cloudfs/cms";
  }
}
