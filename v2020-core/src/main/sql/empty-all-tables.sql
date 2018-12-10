-- Empty all tables in database
DELETE FROM element_property;
DELETE FROM link_property;
DELETE FROM link;
update element set scope_uuid = null, parent_uuid = null;
DELETE FROM element;
