
create index element_uuid_idx on element (uuid);
create index element_scope_uuid_idx on element (scope_uuid);
create index element_parent_uuid_idx on element (parent_uuid);
create index element_property_uuid_idx on element_property (uuid);
create index element_property_element_uuid_idx on element_property (element_uuid);
create index link_property_uuid_idx on link_property (uuid);
create index link_property_property_link_uuid_idx on link_property (link_uuid);
create index link_uuid_idx on link (uuid);
create index link_source_uuid_idx on link (source_uuid);
create index link_destination_uuid_idx on link (destination_uuid);

