/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Jonas Jordan
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
package org.veo.persistence.migrations

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

import groovy.sql.Sql

class V122__fix_probabilities extends BaseJavaMigration {

    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            execute('''
update risk_definition_set
set risk_definitions = (select jsonb_object_agg(key,
                                                case
                                                    when (value -> 'probability' -> 'levels' = '[{"ordinalValue":0,"htmlColor":"#12AE0F","translations":{"de":{"name":"ja","abbreviation":"J","description":"Die Maßnahme ist vollständig umgesetzt."},"en":{"name":"yes","abbreviation":"Y","description":"The measure is fully implemented."}}},{"ordinalValue":1,"htmlColor":"#AE0D11","translations":{"de":{"name":"nein","abbreviation":"N","description":"Die Maßnahme ist nicht umgesetzt."},"en":{"name":"no","abbreviation":"N","description":"The measure has not been implemented."}}},{"ordinalValue":2,"htmlColor":"#EDE92F","translations":{"de":{"name":"teilweise","abbreviation":"Tw","description":"Die Maßnahme ist nicht vollständig umgesetzt."},"en":{"name":"partial","abbreviation":"P","description":"The measure is not fully implemented."}}},{"ordinalValue":3,"htmlColor":"#49A2ED","translations":{"de":{"name":"nicht anwendbar","abbreviation":"NA","description":"Die Maßnahme ist für den Betrachtungsgegenstand nicht anwendbar."},"en":{"name":"not applicable","abbreviation":"NA","description":"The measure is not applicable to the subject under consideration."}}}]'::jsonb)
                                                        then value || jsonb_build_object(
                                                            'probability',
                                                            (value -> 'probability') || jsonb_build_object(
                                                                    'levels', case key
                                                                                  when 'BCMRA' then '[{"ordinalValue":0,"htmlColor":"#FFFFFF","translations":{"de":{"name":"","abbreviation":"NA","description":""},"en":{"name":"","abbreviation":"NA","description":""}}}]'::jsonb
                                                                                  when 'DSRA' then '[{"ordinalValue":0,"htmlColor":"#004643","translations":{"de":{"name":"Selten","abbreviation":"1","description":"Ereignis könnte nach heutigem Kenntnisstand höchstens alle fünf Jahre eintreten."},"en":{"name":"Rare","abbreviation":"1","description":"Event could occur at most every five years based on current knowledge."}}},{"ordinalValue":1,"htmlColor":"#004643","translations":{"de":{"name":"Mittel","abbreviation":"2","description":"Ereignis tritt einmal alle fünf Jahre bis einmal im Jahr ein."},"en":{"name":"Medium","abbreviation":"2","description":"Event occurs once every five years to once a year."}}},{"ordinalValue":2,"htmlColor":"#004643","translations":{"de":{"name":"Häufig","abbreviation":"3","description":"Ereignis tritt einmal im Jahr bis einmal pro Monat ein."},"en":{"name":"Frequent","abbreviation":"3","description":"Event occurs once a year to once a month"}}},{"ordinalValue":3,"htmlColor":"#004643","translations":{"de":{"name":"Sehr häufig","abbreviation":"4","description":"Ereignis tritt mehrmals im Monat ein."},"en":{"name":"Very frequent","abbreviation":"4","description":"Event occurs several times a month"}}}]'::jsonb
                                                                                  when 'GSRA' then '[{"ordinalValue":0,"htmlColor":"#004643","translations":{"de":{"name":"Selten","abbreviation":"1","description":"Ereignis könnte nach heutigem Kenntnisstand höchstens alle fünf Jahre eintreten."},"en":{"name":"Rarely","abbreviation":"1","description":"According to present knowledge, the event could occur every 5 years at the most."}}},{"ordinalValue":1,"htmlColor":"#004643","translations":{"de":{"name":"Mittel","abbreviation":"2","description":"Ereignis tritt einmal alle fünf Jahre bis einmal im Jahr ein."},"en":{"name":"Medium","abbreviation":"2","description":"The event occurs once every 5 years to once a year."}}},{"ordinalValue":2,"htmlColor":"#004643","translations":{"de":{"name":"Häufig","abbreviation":"3","description":"Ereignis tritt einmal im Jahr bis einmal pro Monat ein."},"en":{"name":"Frequently","abbreviation":"3","description":"The event occurs once a year to once a month."}}},{"ordinalValue":3,"htmlColor":"#004643","translations":{"de":{"name":"Sehr häufig","abbreviation":"4","description":"Ereignis tritt mehrmals im Monat ein."},"en":{"name":"Very frequently","abbreviation":"4","description":"The event occurs several times a month."}}}]'::jsonb
                                                                                  when 'GSRD' then '[{"ordinalValue":0,"htmlColor":"#8DBAFF","translations":{"de":{"name":"Selten","abbreviation":"J","description":"Ereignis könnte nach heutigem Kenntnisstand höchstens alle fünf Jahre eintreten."},"en":{"name":"Rare","abbreviation":"Y","description":"The measure is fully implemented."}}},{"ordinalValue":1,"htmlColor":"#689AF4","translations":{"de":{"name":"Mittel","abbreviation":"N","description":"Ereignis tritt einmal alle fünf Jahre bis einmal im Jahr ein."},"en":{"name":"Medium","abbreviation":"N","description":"The measure has not been implemented."}}},{"ordinalValue":2,"htmlColor":"#417BD2","translations":{"de":{"name":"Häufig","abbreviation":"Tw","description":"Ereignis tritt einmal im Jahr bis einmal pro Monat ein."},"en":{"name":"Frequent","abbreviation":"P","description":"The measure is not fully implemented."}}},{"ordinalValue":3,"htmlColor":"#025DB0","translations":{"de":{"name":"Sehr häufig","abbreviation":"NA","description":"Ereignis tritt mehrmals im Monat ein."},"en":{"name":"Very frequent","abbreviation":"NA","description":"The measure is not applicable to the subject under consideration."}}}]'::jsonb
                                                                                  when 'ISORA' then '[{"ordinalValue":0,"htmlColor":"#8DBAFF","translations":{"de":{"name":"Selten","abbreviation":"1","description":"Ereignis könnte nach heutigem Kenntnisstand höchstens alle fünf Jahre eintreten."},"en":{"name":"Rare","abbreviation":"1","description":"Event could occur at most every five years based on current knowledge."}}},{"ordinalValue":1,"htmlColor":"#699AF4","translations":{"de":{"name":"Mittel","abbreviation":"2","description":"Ereignis tritt einmal alle fünf Jahre bis einmal im Jahr ein."},"en":{"name":"Medium","abbreviation":"2","description":"Event occurs once every five years to once a year."}}},{"ordinalValue":2,"htmlColor":"#427BD2","translations":{"de":{"name":"Häufig","abbreviation":"3","description":"Ereignis tritt einmal im Jahr bis einmal pro Monat ein."},"en":{"name":"Frequent","abbreviation":"3","description":"Event occurs once a year to once a month"}}},{"ordinalValue":3,"htmlColor":"#045DB0","translations":{"de":{"name":"Sehr häufig","abbreviation":"4","description":"Ereignis tritt mehrmals im Monat ein."},"en":{"name":"Very frequent","abbreviation":"4","description":"Event occurs several times a month"}}}]'::jsonb
                                                                                  else value -> 'probability' -> 'levels'
                                                                        end))
                                                    else value end)
                        from jsonb_each(risk_definitions))
where risk_definitions != '{}'::jsonb;
''')
        }
    }
}
