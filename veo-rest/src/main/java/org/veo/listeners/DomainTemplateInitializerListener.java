/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan.
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
package org.veo.listeners;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import org.veo.adapter.service.domaintemplate.events.DomainServiceReinitializeEvent;
import org.veo.core.usecase.UseCase.EmptyInput;
import org.veo.core.usecase.UseCaseInteractor;
import org.veo.core.usecase.domaintemplate.DomainTemplateInitalizeUseCase;

import lombok.extern.slf4j.Slf4j;

/**
 * Listens to {@link ApplicationReadyEvent} and
 * {@link DomainServiceReinitializeEvent} to initialize or reinitialize the
 * domaintemplateservice.
 */
@Component
@Slf4j
public class DomainTemplateInitializerListener {

    @Autowired
    private DomainTemplateInitalizeUseCase initalizer;
    @Autowired
    private UseCaseInteractor useCaseInteractor;

    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Initalize DomainTemplateService at startup");
        useCaseInteractor.execute(initalizer, EmptyInput.INSTANCE, e -> null);
    }

    @EventListener
    public void onReinitEvent(DomainServiceReinitializeEvent event) {
        log.info("Reinitalize DomainTemplateService");
        useCaseInteractor.execute(initalizer, EmptyInput.INSTANCE, e -> null);
    }
}