-- Nacos dev/prod CSV restore helper
-- ? psql ???????????????? CSV ?????????
-- CSV ?? NULL \N???? NULL ?????????

\copy public.tenant_info FROM 'tenant_info.csv' WITH (FORMAT csv, HEADER true, NULL '\N');
\copy public.tenant_capacity FROM 'tenant_capacity.csv' WITH (FORMAT csv, HEADER true, NULL '\N');
\copy public.config_info FROM 'config_info.csv' WITH (FORMAT csv, HEADER true, NULL '\N');
\copy public.config_info_gray FROM 'config_info_gray.csv' WITH (FORMAT csv, HEADER true, NULL '\N');
\copy public.config_tags_relation FROM 'config_tags_relation.csv' WITH (FORMAT csv, HEADER true, NULL '\N');
\copy public.his_config_info FROM 'his_config_info.csv' WITH (FORMAT csv, HEADER true, NULL '\N');
\copy public.ai_resource FROM 'ai_resource.csv' WITH (FORMAT csv, HEADER true, NULL '\N');
\copy public.ai_resource_version FROM 'ai_resource_version.csv' WITH (FORMAT csv, HEADER true, NULL '\N');
\copy public.pipeline_execution FROM 'pipeline_execution.csv' WITH (FORMAT csv, HEADER true, NULL '\N');
\copy public.group_capacity FROM 'group_capacity.csv' WITH (FORMAT csv, HEADER true, NULL '\N');
