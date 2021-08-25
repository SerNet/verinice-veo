/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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
package org.veo.core.entity.util;

import java.util.Optional;

import org.veo.core.entity.Asset;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.CustomProperties;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Unit;

/**
 * The model switch provides cases for all veo types.
 *
 * @param <T>
 *            - the return type
 */
public abstract class ModelSwitch<T> {

    public Optional<T> doOptionalSwitch(Object o) {
        return Optional.ofNullable(doSwitch(o));
    }

    public T doSwitch(Object o) {
        if (o instanceof EntityLayerSupertype) {
            T object = caseEntityLayerSupertype((EntityLayerSupertype) o);
            if (object != null)
                return object;
        }
        if (o instanceof Person) {
            T object = casePerson((Person) o);
            if (object != null)
                return object;
        }
        if (o instanceof Asset) {
            T object = caseAsset((Asset) o);
            if (object != null)
                return object;
        }
        if (o instanceof Process) {
            T object = caseProcess((Process) o);
            if (object != null)
                return object;
        }
        if (o instanceof Document) {
            T object = caseDocument((Document) o);
            if (object != null)
                return object;
        }
        if (o instanceof Control) {
            T object = caseControl((Control) o);
            if (object != null)
                return object;
        }
        if (o instanceof Client) {
            T object = caseClient((Client) o);
            if (object != null)
                return object;
        }
        if (o instanceof Domain) {
            T object = caseDomain((Domain) o);
            if (object != null)
                return object;
        }
        if (o instanceof Nameable) {
            T object = caseNameable((Nameable) o);
            if (object != null)
                return object;
        }
        if (o instanceof Unit) {
            T object = caseUnit((Unit) o);
            if (object != null)
                return object;
        }
        if (o instanceof CustomLink) {
            T object = caseCustomLink((CustomLink) o);
            if (object != null)
                return object;
        }
        if (o instanceof CustomProperties) {
            T object = caseCustomProperties((CustomProperties) o);
            if (object != null)
                return object;
        }
        return null;
    }

    public T caseEntityLayerSupertype(EntityLayerSupertype object) {
        return null;
    }

    public T casePerson(Person object) {
        return null;
    }

    public T caseAsset(Asset object) {
        return null;
    }

    public T caseProcess(Process object) {
        return null;
    }

    public T caseDocument(Document object) {
        return null;
    }

    public T caseControl(Control object) {
        return null;
    }

    public T caseClient(Client object) {
        return null;
    }

    public T caseDomain(Domain object) {
        return null;
    }

    public T caseNameable(Nameable object) {
        return null;
    }

    public T caseUnit(Unit object) {
        return null;
    }

    public T caseCustomLink(CustomLink object) {
        return null;
    }

    public T caseCustomProperties(CustomProperties object) {
        return null;
    }

}
