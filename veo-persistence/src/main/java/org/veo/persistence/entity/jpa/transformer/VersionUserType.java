/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2026  Jochen Kemnade
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.persistence.entity.jpa.transformer;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

import com.github.zafarkhaja.semver.Version;

public class VersionUserType implements UserType<Version> {

  @Override
  public int getSqlType() {
    return Types.VARCHAR;
  }

  @Override
  public Class<Version> returnedClass() {
    return Version.class;
  }

  @Override
  public boolean isMutable() {
    return false;
  }

  @Override
  public Version deepCopy(Version value) {
    return value;
  }

  @Override
  public Version nullSafeGet(ResultSet rs, int position, WrapperOptions options)
      throws SQLException {
    String s = rs.getString(position);
    return s == null ? null : Version.parse(s);
  }

  @Override
  public void nullSafeSet(PreparedStatement st, Version value, int position, WrapperOptions options)
      throws SQLException {
    if (value == null) {
      st.setNull(position, Types.VARCHAR);
    } else {
      st.setString(position, value.toString());
    }
  }

  @Override
  public boolean equals(Version x, Version y) {
    return Objects.equals(x, y);
  }

  @Override
  public int hashCode(Version x) {
    return x.hashCode();
  }

  @Override
  public Serializable disassemble(Version value) {
    return value;
  }

  @Override
  public Version assemble(Serializable cached, Object owner) {
    return (Version) cached;
  }

  @Override
  public Version replace(Version detached, Version managed, Object owner) {
    return detached;
  }
}
