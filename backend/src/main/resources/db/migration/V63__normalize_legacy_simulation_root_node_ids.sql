UPDATE simulation_versions
SET root_node_id = 'turno-' || SUBSTRING(root_node_id FROM 7)
WHERE root_node_id LIKE 'etapa-%';
