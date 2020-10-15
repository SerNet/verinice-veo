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

import org.veo.adapter.ModelObjectReferenceResolver;
import org.veo.adapter.persistence.schema.EntitySchemaServiceClassPathImpl;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityContextFactory;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.service.EntitySchemaService;
import org.veo.core.usecase.asset.CreateAssetUseCase;
import org.veo.core.usecase.asset.GetAssetUseCase;
import org.veo.core.usecase.asset.GetAssetsUseCase;
import org.veo.core.usecase.asset.UpdateAssetUseCase;
import org.veo.core.usecase.base.DeleteEntityUseCase;
import org.veo.core.usecase.base.UnitHierarchyProvider;
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
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory;

/**
 * This configuration takes care of wiring classes from core modules
 * (Entity-Layer, Use-Case-Layer) that have no dependency to the Spring
 * framework. They are therefore not picked up and autowired by Spring.
 */
@Configuration
public class ModuleConfiguration {

    @Bean
    public CreateAssetUseCase createAssetUseCase(UnitRepositoryImpl unitRepository,
            AssetRepositoryImpl assetRepository) {
        return new CreateAssetUseCase(unitRepository, assetRepository);
    }

    @Bean
    public GetAssetUseCase getAssetUseCase(AssetRepositoryImpl assetRepository) {
        return new GetAssetUseCase(assetRepository);
    }

    @Bean
    public GetAssetsUseCase getAssetsUseCase(ClientRepositoryImpl clientRepository,
            AssetRepositoryImpl assetRepository, UnitHierarchyProvider unitHierarchyProvider) {
        return new GetAssetsUseCase(clientRepository, assetRepository, unitHierarchyProvider);
    }

    @Bean
    public UpdateAssetUseCase updateAssetUseCase(AssetRepositoryImpl assetRepository) {
        return new UpdateAssetUseCase(assetRepository);
    }

    @Bean
    public CreateControlUseCase createControlUseCase(UnitRepositoryImpl unitRepository,
            ControlRepositoryImpl controlRepository) {
        return new CreateControlUseCase(unitRepository, controlRepository);
    }

    @Bean
    public GetControlUseCase getControlUseCase(ControlRepositoryImpl controlRepository) {
        return new GetControlUseCase(controlRepository);
    }

    @Bean
    public GetControlsUseCase getControlsUseCase(ClientRepositoryImpl clientRepository,
            ControlRepositoryImpl controlRepository, UnitHierarchyProvider unitHierarchyProvider) {
        return new GetControlsUseCase(clientRepository, controlRepository, unitHierarchyProvider);
    }

    @Bean
    public UpdateControlUseCase updateControlUseCase(ControlRepositoryImpl controlRepository) {
        return new UpdateControlUseCase(controlRepository);
    }

    @Bean
    public CreateProcessUseCase createProcessUseCase(UnitRepositoryImpl unitRepository,
            ProcessRepositoryImpl processRepository) {
        return new CreateProcessUseCase(unitRepository, processRepository);
    }

    @Bean
    public GetProcessUseCase getProcessUseCase(ProcessRepositoryImpl processRepository) {
        return new GetProcessUseCase(processRepository);
    }

    @Bean
    public UpdateProcessUseCase putProcessUseCase(ProcessRepositoryImpl processRepository) {
        return new UpdateProcessUseCase(processRepository);
    }

    @Bean
    public GetUnitUseCase getUnitUseCase(UnitRepositoryImpl repository) {
        return new GetUnitUseCase(repository);
    }

    @Bean
    public GetUnitsUseCase getUnitsUseCase(ClientRepository repository,
            UnitRepositoryImpl unitRepository) {
        return new GetUnitsUseCase(repository, unitRepository);
    }

    @Bean
    public GetProcessesUseCase getProcessesUseCase(ClientRepository clientRepository,
            ProcessRepository processRepository, UnitHierarchyProvider unitHierarchyProvider) {
        return new GetProcessesUseCase(clientRepository, processRepository, unitHierarchyProvider);
    }

    @Bean
    public UpdateUnitUseCase getPutUnitUseCase(UnitRepositoryImpl repository) {
        return new UpdateUnitUseCase(repository);
    }

    @Bean
    public CreateUnitUseCase getCreateUnitUseCase(ClientRepositoryImpl clientRepository,
            UnitRepositoryImpl unitRepository) {
        return new CreateUnitUseCase(clientRepository, unitRepository, getEntityFactory());
    }

    @Bean
    public DeleteUnitUseCase getDeleteUnitUseCase(ClientRepositoryImpl clientRepository,
            UnitRepositoryImpl unitRepository, RepositoryProvider repositoryProvider) {
        return new DeleteUnitUseCase(clientRepository, unitRepository, repositoryProvider);
    }

    @Bean
    public CreatePersonUseCase createPersonUseCase(UnitRepositoryImpl unitRepository,
            PersonRepositoryImpl personRepository) {
        return new CreatePersonUseCase(unitRepository, personRepository);
    }

    @Bean
    public GetPersonUseCase getPersonUseCase(PersonRepositoryImpl personRepository) {
        return new GetPersonUseCase(personRepository);
    }

    @Bean
    public GetPersonsUseCase getPersonsUseCase(ClientRepositoryImpl clientRepository,
            PersonRepositoryImpl personRepository, UnitHierarchyProvider unitHierarchyProvider) {
        return new GetPersonsUseCase(clientRepository, personRepository, unitHierarchyProvider);
    }

    @Bean
    public UpdatePersonUseCase updatePersonUseCase(PersonRepositoryImpl personRepository) {
        return new UpdatePersonUseCase(personRepository);
    }

    @Bean
    public CreateGroupUseCase getCreateGroupUseCase(UnitRepository unitRepository,
            RepositoryProvider repositoryProvider) {
        return new CreateGroupUseCase(unitRepository, repositoryProvider, getEntityFactory());
    }

    @Bean
    public GetGroupUseCase getGroupUseCase(RepositoryProvider repositoryProvider,
            UnitRepository unitRepository) {
        return new GetGroupUseCase(repositoryProvider);
    }

    @Bean
    public GetGroupsUseCase getGroupsUseCase(ClientRepositoryImpl clientRepository,
            UnitHierarchyProvider unitHierarchyProvider, RepositoryProvider repositoryProvider) {
        return new GetGroupsUseCase(clientRepository, repositoryProvider, unitHierarchyProvider);
    }

    @Bean
    public DeleteGroupUseCase getDeleteGroupUseCase(RepositoryProvider repositoryProvider) {
        return new DeleteGroupUseCase(repositoryProvider);
    }

    @Bean
    public PutGroupUseCase putGroupUseCase(RepositoryProvider repositoryProvider) {
        return new PutGroupUseCase(repositoryProvider);
    }

    @Bean
    public DeleteEntityUseCase deleteEntityUseCase(RepositoryProvider repositoryProvider) {
        return new DeleteEntityUseCase(repositoryProvider);

    }

    @Bean
    public EntitySchemaService getSchemaService() {
        return new EntitySchemaServiceClassPathImpl();
    }

    @Bean
    public EntityFactory getEntityFactory() {
        return new EntityDataFactory();
    }

    @Bean
    public UnitHierarchyProvider unitHierarchyProvider(UnitRepository unitRepository) {
        return new UnitHierarchyProvider(unitRepository);
    }

    @Bean
    public DtoToEntityContextFactory dtoToEntityContextFactory(EntityFactory entityFactory,
            EntitySchemaService entitySchemaService) {
        return new DtoToEntityContextFactory(entityFactory, entitySchemaService);
    }

    @Bean
    public ModelObjectReferenceResolver modelObjectReferenceResolver(
            RepositoryProvider repositoryProvider,
            DtoToEntityContextFactory dtoToEntityContextFactory) {
        return new ModelObjectReferenceResolver(repositoryProvider, dtoToEntityContextFactory);
    }
}
