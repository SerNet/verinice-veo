/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.veo.persistence;

import org.springframework.data.repository.CrudRepository;
import org.veo.model.Link;

/**
 *
 * @author Daniel Murygin
 */
public interface LinkRepository extends CrudRepository<Link, String> {

}
