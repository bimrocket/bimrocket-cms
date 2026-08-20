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

import io.pebbletemplates.pebble.loader.FileLoader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 *
 * @author realor
 */
public class ContentLoader extends FileLoader
{
  public ContentLoader(String prefix)
  {
    super(prefix);
  }

  public String getResourceAsString(String pathName)
    throws IOException
  {
    Path path = Path.of(getPrefix(), pathName);
    return Files.readString(path);
  }

  public void copyResource(String pathName, OutputStream out)
    throws IOException
  {
    Path path = Path.of(getPrefix(), pathName);
    Files.copy(path, out);
  }

  public long getSize(String pathName) throws IOException
  {
    Path path = Path.of(getPrefix(), pathName);
    return Files.size(path);
  }

  public long getLastModified(String pathName) throws IOException
  {
    Path path = Path.of(getPrefix(), pathName);
    return Files.getLastModifiedTime(path).toMillis();
  }

  public List<String> getChildNames(String dirPathName)
    throws IOException
  {
    return getChildNames(dirPathName, p -> true);
  }

  public List<String> getChildNames(
    String dirPathName, Predicate<String> predicate) throws IOException
  {
    Path path = Path.of(getPrefix(), dirPathName);
    if (!Files.isDirectory(path)) return Collections.emptyList();

    return Files.list(path)
      .map(p -> p.getFileName().toString())
      .filter(predicate)
      .toList();
  }
}
