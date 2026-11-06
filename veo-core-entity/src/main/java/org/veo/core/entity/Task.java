/*
 * verinice.veo
 * Copyright (C) 2026  Jonas Jordan
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
 */
package org.veo.core.entity;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import javax.annotation.Nullable;

import org.veo.core.entity.compliance.RequirementImplementation;
import org.veo.core.entity.inspection.Severity;

public record Task(
    @NotNull TaskType type,
    @Nullable Person assignee,
    @Nullable LocalDate deadline,
    @Nullable RequirementImplementation requirementImplementation,
    @Nullable Element element,
    @Nullable Severity severity,
    @Nullable TranslatedText summary) {}
