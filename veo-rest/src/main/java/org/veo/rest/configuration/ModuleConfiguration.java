/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
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
package org.veo.rest.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.veo.adapter.persistence.schema.EntitySchemaServiceClassPathImpl;
import org.veo.core.entity.transform.TransformContextProvider;
import org.veo.core.service.EntitySchemaService;
import org.veo.core.usecase.asset.CreateAssetUseCase;
import org.veo.core.usecase.asset.GetAssetUseCase;
import org.veo.core.usecase.asset.GetAssetsUseCase;
import org.veo.core.usecase.asset.UpdateAssetUseCase;
import org.veo.core.usecase.base.DeleteEntityUseCase;
import org.veo.core.usecase.control.CreateControlUseCase;
import org.veo.core.usecase.control.GetControlUseCase;
import org.veo.core.usecase.control.GetControlsUseCase;
import org.veo.core.usecase.control.UpdateControlUseCase;
import org.veo.core.usecase.group.CreateGroupUseCase;
import org.veo.core.usecase.group.DeleteGroupUseCase;
import org.veo.core.usecase.group.GetGroupUseCase;
import org.veo.core.usecase.group.GetGroupsUseCase;
import org.veo.core.usecase.group.PutGroupUseCase;
import org.veo.core.usecase.person.CreatePersonUseCase;
import org.veo.core.usecase.person.GetPersonUseCase;
import org.veo.core.usecase.person.GetPersonsUseCase;
import org.veo.core.usecase.person.UpdatePersonUseCase;
import org.veo.core.usecase.process.CreateProcessUseCase;
import org.veo.core.usecase.process.GetProcessUseCase;
import org.veo.core.usecase.process.GetProcessesUseCase;
import org.veo.core.usecase.process.UpdateProcessUseCase;
import org.veo.core.usecase.repository.ClientRepository;
import org.veo.core.usecase.repository.ProcessRepository;
import org.veo.core.usecase.repository.RepositoryProvider;
import org.veo.core.usecase.repository.UnitRepository;
import org.veo.core.usecase.unit.CreateUnitUseCase;
import org.veo.core.usecase.unit.DeleteUnitUseCase;
import org.veo.core.usecase.unit.GetUnitUseCase;
import org.veo.core.usecase.unit.GetUnitsUseCase;
import org.veo.core.usecase.unit.UpdateUnitUseCase;
import org.veo.persistence.access.AssetRepositoryImpl;
import org.veo.persistence.access.ClientRepositoryImpl;
import org.veo.persistence.access.ControlRepositoryImpl;
import org.veo.persistence.access.PersonRepositoryImpl;
import org.veo.persistence.access.ProcessRepositoryImpl;
import org.veo.persistence.access.UnitRepositoryImpl;

/**
 * This configuration takes care of wiring classes from core modules
 * (Entity-Layer, Use-Case-Layer) that have no dependency to the Spring
 * framework. They are therefore not picked up and autowired by Spring.
 */
@Configuration
public class ModuleConfiguration {

    @Bean
    public CreateAssetUseCase createAssetUseCase(UnitRepositoryImpl unitRepository,
            AssetRepositoryImpl assetRepository,
            TransformContextProvider transformContextProvider) {
        return new CreateAssetUseCase(unitRepository, assetRepository, transformContextProvider);
    }

    @Bean
    public GetAssetUseCase getAssetUseCase(AssetRepositoryImpl assetRepository,
            TransformContextProvider transformContextProvider) {
        return new GetAssetUseCase(assetRepository, transformContextProvider);
    }

    @Bean
    public GetAssetsUseCase getAssetsUseCase(ClientRepositoryImpl clientRepository,
            AssetRepositoryImpl assetRepository,
            TransformContextProvider transformContextProvider) {
        return new GetAssetsUseCase(clientRepository, assetRepository);
    }

    @Bean
    public UpdateAssetUseCase updateAssetUseCase(AssetRepositoryImpl assetRepository,
            TransformContextProvider transformContextProvider) {
        return new UpdateAssetUseCase(assetRepository, transformContextProvider);
    }

    @Bean
    public CreateControlUseCase createControlUseCase(UnitRepositoryImpl unitRepository,
            ControlRepositoryImpl controlRepository,
            TransformContextProvider transformContextProvider) {
        return new CreateControlUseCase(unitRepository, controlRepository,
                transformContextProvider);
    }

    @Bean
    public GetControlUseCase getControlUseCase(ControlRepositoryImpl controlRepository,
            TransformContextProvider transformContextProvider) {
        return new GetControlUseCase(controlRepository, transformContextProvider);
    }

    @Bean
    public GetControlsUseCase getControlsUseCase(ClientRepositoryImpl clientRepository,
            ControlRepositoryImpl controlRepository,
            TransformContextProvider transformContextProvider) {
        return new GetControlsUseCase(clientRepository, controlRepository);
    }

    @Bean
    public UpdateControlUseCase updateControlUseCase(ControlRepositoryImpl controlRepository,
            TransformContextProvider transformContextProvider) {
        return new UpdateControlUseCase(controlRepository, transformContextProvider);
    }

    @Bean
    public CreateProcessUseCase createProcessUseCase(UnitRepositoryImpl unitRepository,
            ProcessRepositoryImpl processRepository,
            TransformContextProvider transformContextProvider) {
        return new CreateProcessUseCase(unitRepository, processRepository,
                transformContextProvider);
    }

    @Bean
    public GetProcessUseCase getProcessUseCase(ProcessRepositoryImpl processRepository) {
        return new GetProcessUseCase(processRepository);
    }

    @Bean
    public UpdateProcessUseCase putProcessUseCase(ProcessRepositoryImpl processRepository,
            TransformContextProvider transformContextProvider) {
        return new UpdateProcessUseCase(processRepository, transformContextProvider);
    }

    @Bean
    public GetUnitUseCase getUnitUseCase(UnitRepositoryImpl repository,
            TransformContextProvider transformContextProvider) {
        return new GetUnitUseCase(repository, transformContextProvider);
    }

    @Bean
    public GetUnitsUseCase getUnitsUseCase(ClientRepository repository,
            TransformContextProvider transformContextProvider) {
        return new GetUnitsUseCase(repository);
    }

    @Bean
    public GetProcessesUseCase getProcessesUseCase(ClientRepository clientRepository,
            ProcessRepository processRepository) {
        return new GetProcessesUseCase(clientRepository, processRepository);
    }

    @Bean
    public UpdateUnitUseCase getPutUnitUseCase(UnitRepositoryImpl repository,
            TransformContextProvider transformContextProvider) {
        return new UpdateUnitUseCase(repository, transformContextProvider);
    }

    @Bean
    public CreateUnitUseCase getCreateUnitUseCase(ClientRepositoryImpl clientRepository) {
        return new CreateUnitUseCase(clientRepository);
    }

    @Bean
    public DeleteUnitUseCase getDeleteUnitUseCase(ClientRepositoryImpl clientRepository,
            UnitRepositoryImpl unitRepository, TransformContextProvider transformContextProvider,
            RepositoryProvider repositoryProvider) {
        return new DeleteUnitUseCase(clientRepository, unitRepository, transformContextProvider,
                repositoryProvider);
    }

    @Bean
    public CreatePersonUseCase createPersonUseCase(UnitRepositoryImpl unitRepository,
            PersonRepositoryImpl personRepository,
            TransformContextProvider transformContextProvider) {
        return new CreatePersonUseCase(unitRepository, personRepository, transformContextProvider);
    }

    @Bean
    public GetPersonUseCase getPersonUseCase(PersonRepositoryImpl personRepository,
            TransformContextProvider transformContextProvider) {
        return new GetPersonUseCase(personRepository, transformContextProvider);
    }

    @Bean
    public GetPersonsUseCase getPersonsUseCase(ClientRepositoryImpl clientRepository,
            PersonRepositoryImpl personRepository) {
        return new GetPersonsUseCase(clientRepository, personRepository);
    }

    @Bean
    public UpdatePersonUseCase updatePersonUseCase(PersonRepositoryImpl personRepository,
            TransformContextProvider transformContextProvider) {
        return new UpdatePersonUseCase(personRepository, transformContextProvider);
    }

    @Bean
    public CreateGroupUseCase getCreateGroupUseCase(UnitRepository unitRepository,
            RepositoryProvider repositoryProvider,
            TransformContextProvider transformContextProvider) {
        return new CreateGroupUseCase(unitRepository, repositoryProvider, transformContextProvider);
    }

    @Bean
    public GetGroupUseCase getGroupUseCase(RepositoryProvider repositoryProvider,
            UnitRepository unitRepository, TransformContextProvider transformContextProvider) {
        return new GetGroupUseCase(repositoryProvider, transformContextProvider);
    }

    @Bean
    public GetGroupsUseCase getGroupsUseCase(ClientRepositoryImpl clientRepository,
            RepositoryProvider repositoryProvider) {
        return new GetGroupsUseCase(clientRepository, repositoryProvider);
    }

    @Bean
    public DeleteGroupUseCase getDeleteGroupUseCase(RepositoryProvider repositoryProvider) {
        return new DeleteGroupUseCase(repositoryProvider);
    }

    @Bean
    public PutGroupUseCase putGroupUseCase(RepositoryProvider repositoryProvider,
            TransformContextProvider transformContextProvider) {
        return new PutGroupUseCase(repositoryProvider, transformContextProvider);
    }

    @Bean
    public DeleteEntityUseCase deleteEntityUseCase(RepositoryProvider repositoryProvider) {
        return new DeleteEntityUseCase(repositoryProvider);

    }

    @Bean
    public EntitySchemaService getSchemaService() {
        return new EntitySchemaServiceClassPathImpl();
    }
}
