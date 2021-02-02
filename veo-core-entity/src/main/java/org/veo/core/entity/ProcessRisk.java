/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core.entity;

/**
 * Based on the definition of the term 'risk' from NIST 800-37:
 * <p>
 * "Risk: A measure of the extent to which an entity is threatened by a
 * potential circumstance or event, [...]",
 * <p>
 * this class links an process ('entity' in the above definition) to a scenario
 * ('circumstance or event').
 * <p>
 * The process may be representing a single process or a group of processes to
 * facilitate modelling subprocesses affected by the observed risk.
 */
public interface ProcessRisk extends AbstractRisk<Process, ProcessRisk> {

}
