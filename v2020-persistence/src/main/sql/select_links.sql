SELECT element.title AS source, e2.title AS dest FROM element 
JOIN link ON element.uuid = link.source_uuid
JOIN element e2 ON e2.uuid = link.destination_uuid
WHERE link.source_uuid = '6e50c5c4-2b6b-4f40-b189-6bc1b5e77352'
