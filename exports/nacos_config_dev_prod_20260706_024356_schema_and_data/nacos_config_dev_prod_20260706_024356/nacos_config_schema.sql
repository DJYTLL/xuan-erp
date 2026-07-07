--
-- PostgreSQL database dump
--

\restrict 1aYnph3VbgdfjJV68WCH4UAJcZAwlS8sXkXFGiGZPcXOjm6FJdj0tUh4LnB0hLp

-- Dumped from database version 16.14
-- Dumped by pg_dump version 16.14

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA public;


--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON SCHEMA public IS 'standard public schema';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: ai_resource; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ai_resource (
    id bigint NOT NULL,
    gmt_create timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    name character varying(256) NOT NULL,
    type character varying(32) NOT NULL,
    c_desc character varying(1024),
    status character varying(32),
    namespace_id character varying(128) DEFAULT ''::character varying NOT NULL,
    biz_tags character varying(1024),
    ext text,
    c_from character varying(256) DEFAULT 'local'::character varying NOT NULL,
    version_info text,
    meta_version bigint DEFAULT 1 NOT NULL,
    scope character varying(16) DEFAULT 'PRIVATE'::character varying NOT NULL,
    owner character varying(128) DEFAULT ''::character varying NOT NULL,
    download_count bigint DEFAULT 0 NOT NULL
);


--
-- Name: TABLE ai_resource; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.ai_resource IS 'AI资源元数据表';


--
-- Name: COLUMN ai_resource.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource.id IS 'id';


--
-- Name: COLUMN ai_resource.gmt_create; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource.gmt_create IS '创建时间';


--
-- Name: COLUMN ai_resource.gmt_modified; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource.gmt_modified IS '修改时间';


--
-- Name: COLUMN ai_resource.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource.name IS '资源名称';


--
-- Name: COLUMN ai_resource.type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource.type IS '资源类型';


--
-- Name: COLUMN ai_resource.c_desc; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource.c_desc IS '资源描述';


--
-- Name: COLUMN ai_resource.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource.status IS '资源状态';


--
-- Name: COLUMN ai_resource.namespace_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource.namespace_id IS '命名空间ID';


--
-- Name: COLUMN ai_resource.biz_tags; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource.biz_tags IS '业务标签';


--
-- Name: COLUMN ai_resource.ext; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource.ext IS '扩展信息(JSON)';


--
-- Name: COLUMN ai_resource.c_from; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource.c_from IS '来源标识(导入/同步来源)';


--
-- Name: COLUMN ai_resource.version_info; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource.version_info IS '版本信息(JSON)';


--
-- Name: COLUMN ai_resource.meta_version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource.meta_version IS '元数据版本(乐观锁)';


--
-- Name: COLUMN ai_resource.scope; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource.scope IS '可见性: PUBLIC/PRIVATE';


--
-- Name: COLUMN ai_resource.owner; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource.owner IS '创建者用户名';


--
-- Name: COLUMN ai_resource.download_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource.download_count IS '下载次数';


--
-- Name: ai_resource_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ai_resource_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ai_resource_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ai_resource_id_seq OWNED BY public.ai_resource.id;


--
-- Name: ai_resource_version; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ai_resource_version (
    id bigint NOT NULL,
    gmt_create timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    type character varying(32) NOT NULL,
    author character varying(128),
    name character varying(256) NOT NULL,
    c_desc character varying(1024),
    status character varying(32) NOT NULL,
    version character varying(64) NOT NULL,
    namespace_id character varying(128) DEFAULT ''::character varying NOT NULL,
    storage text,
    publish_pipeline_info text,
    download_count bigint DEFAULT 0 NOT NULL
);


--
-- Name: TABLE ai_resource_version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.ai_resource_version IS 'AI资源版本表';


--
-- Name: COLUMN ai_resource_version.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource_version.id IS 'id';


--
-- Name: COLUMN ai_resource_version.gmt_create; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource_version.gmt_create IS '创建时间';


--
-- Name: COLUMN ai_resource_version.gmt_modified; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource_version.gmt_modified IS '修改时间';


--
-- Name: COLUMN ai_resource_version.type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource_version.type IS '资源类型';


--
-- Name: COLUMN ai_resource_version.author; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource_version.author IS '作者';


--
-- Name: COLUMN ai_resource_version.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource_version.name IS '资源名称';


--
-- Name: COLUMN ai_resource_version.c_desc; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource_version.c_desc IS '版本描述';


--
-- Name: COLUMN ai_resource_version.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource_version.status IS '版本状态';


--
-- Name: COLUMN ai_resource_version.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource_version.version IS '版本号';


--
-- Name: COLUMN ai_resource_version.namespace_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource_version.namespace_id IS '命名空间ID';


--
-- Name: COLUMN ai_resource_version.storage; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource_version.storage IS '存储信息(JSON)';


--
-- Name: COLUMN ai_resource_version.publish_pipeline_info; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource_version.publish_pipeline_info IS '发布流水线信息(JSON)';


--
-- Name: COLUMN ai_resource_version.download_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ai_resource_version.download_count IS '下载次数';


--
-- Name: ai_resource_version_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ai_resource_version_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ai_resource_version_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ai_resource_version_id_seq OWNED BY public.ai_resource_version.id;


--
-- Name: config_info; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_info (
    id bigint NOT NULL,
    data_id character varying(255) NOT NULL,
    group_id character varying(255),
    content text NOT NULL,
    md5 character varying(32),
    gmt_create timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    src_user text,
    src_ip character varying(20),
    app_name character varying(128),
    tenant_id character varying(128) DEFAULT ''::character varying NOT NULL,
    c_desc character varying(256),
    c_use character varying(64),
    effect character varying(64),
    type character varying(64),
    c_schema text,
    encrypted_data_key text NOT NULL
);


--
-- Name: TABLE config_info; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.config_info IS 'config_info';


--
-- Name: COLUMN config_info.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_info.id IS 'id';


--
-- Name: COLUMN config_info.data_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_info.data_id IS 'data_id';


--
-- Name: COLUMN config_info.content; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_info.content IS 'content';


--
-- Name: COLUMN config_info.md5; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_info.md5 IS 'md5';


--
-- Name: COLUMN config_info.gmt_create; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_info.gmt_create IS '创建时间';


--
-- Name: COLUMN config_info.gmt_modified; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_info.gmt_modified IS '修改时间';


--
-- Name: COLUMN config_info.src_user; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_info.src_user IS 'source user';


--
-- Name: COLUMN config_info.src_ip; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_info.src_ip IS 'source ip';


--
-- Name: COLUMN config_info.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_info.tenant_id IS '租户字段';


--
-- Name: COLUMN config_info.encrypted_data_key; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_info.encrypted_data_key IS '秘钥';


--
-- Name: config_info_gray; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_info_gray (
    id bigint NOT NULL,
    data_id character varying(255) NOT NULL,
    group_id character varying(128) NOT NULL,
    content text NOT NULL,
    md5 character varying(32),
    src_user text,
    src_ip character varying(100),
    gmt_create timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    app_name character varying(128),
    tenant_id character varying(128) DEFAULT ''::character varying NOT NULL,
    gray_name character varying(128) NOT NULL,
    gray_rule text NOT NULL,
    encrypted_data_key character varying(256) DEFAULT ''::character varying NOT NULL
);


--
-- Name: TABLE config_info_gray; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.config_info_gray IS 'config_info_gray';


--
-- Name: COLUMN config_info_gray.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_info_gray.id IS 'id';


--
-- Name: COLUMN config_info_gray.data_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_info_gray.data_id IS 'data_id';


--
-- Name: COLUMN config_info_gray.group_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_info_gray.group_id IS 'group_id';


--
-- Name: COLUMN config_info_gray.content; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_info_gray.content IS 'content';


--
-- Name: COLUMN config_info_gray.md5; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_info_gray.md5 IS 'md5';


--
-- Name: COLUMN config_info_gray.src_user; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_info_gray.src_user IS 'source user';


--
-- Name: COLUMN config_info_gray.src_ip; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_info_gray.src_ip IS 'source ip';


--
-- Name: COLUMN config_info_gray.gmt_create; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_info_gray.gmt_create IS '创建时间';


--
-- Name: COLUMN config_info_gray.gmt_modified; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_info_gray.gmt_modified IS '修改时间';


--
-- Name: COLUMN config_info_gray.app_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_info_gray.app_name IS 'app_name';


--
-- Name: COLUMN config_info_gray.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_info_gray.tenant_id IS '租户字段';


--
-- Name: COLUMN config_info_gray.gray_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_info_gray.gray_name IS '灰度名称';


--
-- Name: COLUMN config_info_gray.gray_rule; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_info_gray.gray_rule IS '灰度规则';


--
-- Name: COLUMN config_info_gray.encrypted_data_key; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_info_gray.encrypted_data_key IS '秘钥';


--
-- Name: config_info_gray_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.config_info_gray_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: config_info_gray_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.config_info_gray_id_seq OWNED BY public.config_info_gray.id;


--
-- Name: config_info_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.config_info_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: config_info_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.config_info_id_seq OWNED BY public.config_info.id;


--
-- Name: config_tags_relation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_tags_relation (
    id bigint NOT NULL,
    tag_name character varying(128) NOT NULL,
    tag_type character varying(64),
    data_id character varying(255) NOT NULL,
    group_id character varying(128) NOT NULL,
    tenant_id character varying(128) DEFAULT ''::character varying NOT NULL,
    nid bigint NOT NULL
);


--
-- Name: TABLE config_tags_relation; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.config_tags_relation IS 'config_tag_relation';


--
-- Name: COLUMN config_tags_relation.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_tags_relation.id IS 'id';


--
-- Name: COLUMN config_tags_relation.tag_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_tags_relation.tag_name IS 'tag_name';


--
-- Name: COLUMN config_tags_relation.tag_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_tags_relation.tag_type IS 'tag_type';


--
-- Name: COLUMN config_tags_relation.data_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_tags_relation.data_id IS 'data_id';


--
-- Name: COLUMN config_tags_relation.group_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_tags_relation.group_id IS 'group_id';


--
-- Name: COLUMN config_tags_relation.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.config_tags_relation.tenant_id IS 'tenant_id';


--
-- Name: config_tags_relation_nid_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.config_tags_relation_nid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: config_tags_relation_nid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.config_tags_relation_nid_seq OWNED BY public.config_tags_relation.nid;


--
-- Name: group_capacity; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.group_capacity (
    id bigint NOT NULL,
    group_id character varying(128) NOT NULL,
    quota integer NOT NULL,
    usage integer NOT NULL,
    max_size integer NOT NULL,
    max_aggr_count integer NOT NULL,
    max_aggr_size integer NOT NULL,
    max_history_count integer DEFAULT 0 NOT NULL,
    gmt_create timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE group_capacity; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.group_capacity IS '集群、各Group容量信息表';


--
-- Name: COLUMN group_capacity.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.group_capacity.id IS '主键ID';


--
-- Name: COLUMN group_capacity.group_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.group_capacity.group_id IS 'Group ID，空字符表示整个集群';


--
-- Name: COLUMN group_capacity.quota; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.group_capacity.quota IS '配额，0表示使用默认值';


--
-- Name: COLUMN group_capacity.usage; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.group_capacity.usage IS '使用量';


--
-- Name: COLUMN group_capacity.max_size; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.group_capacity.max_size IS '单个配置大小上限，单位为字节，0表示使用默认值';


--
-- Name: COLUMN group_capacity.max_aggr_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.group_capacity.max_aggr_count IS '聚合子配置最大个数，，0表示使用默认值';


--
-- Name: COLUMN group_capacity.max_aggr_size; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.group_capacity.max_aggr_size IS '单个聚合数据的子配置大小上限，单位为字节，0表示使用默认值';


--
-- Name: COLUMN group_capacity.max_history_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.group_capacity.max_history_count IS '最大变更历史数量';


--
-- Name: COLUMN group_capacity.gmt_create; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.group_capacity.gmt_create IS '创建时间';


--
-- Name: COLUMN group_capacity.gmt_modified; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.group_capacity.gmt_modified IS '修改时间';


--
-- Name: group_capacity_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.group_capacity_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: group_capacity_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.group_capacity_id_seq OWNED BY public.group_capacity.id;


--
-- Name: his_config_info; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.his_config_info (
    id bigint NOT NULL,
    nid bigint NOT NULL,
    data_id character varying(255) NOT NULL,
    group_id character varying(128) NOT NULL,
    app_name character varying(128),
    content text NOT NULL,
    md5 character varying(32),
    gmt_create timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    src_user text,
    src_ip character varying(20),
    op_type character(10),
    tenant_id character varying(128) DEFAULT ''::character varying NOT NULL,
    encrypted_data_key text NOT NULL,
    publish_type character varying(50) DEFAULT 'formal'::character varying,
    gray_name character varying(50),
    ext_info text
);


--
-- Name: TABLE his_config_info; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.his_config_info IS '多租户改造';


--
-- Name: COLUMN his_config_info.app_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.his_config_info.app_name IS 'app_name';


--
-- Name: COLUMN his_config_info.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.his_config_info.tenant_id IS '租户字段';


--
-- Name: COLUMN his_config_info.encrypted_data_key; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.his_config_info.encrypted_data_key IS '秘钥';


--
-- Name: COLUMN his_config_info.publish_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.his_config_info.publish_type IS 'publish type gray or formal';


--
-- Name: COLUMN his_config_info.gray_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.his_config_info.gray_name IS 'gray name';


--
-- Name: COLUMN his_config_info.ext_info; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.his_config_info.ext_info IS 'ext info';


--
-- Name: his_config_info_nid_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.his_config_info_nid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: his_config_info_nid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.his_config_info_nid_seq OWNED BY public.his_config_info.nid;


--
-- Name: permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.permissions (
    role character varying(50) NOT NULL,
    resource character varying(512) NOT NULL,
    action character varying(8) NOT NULL
);


--
-- Name: pipeline_execution; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pipeline_execution (
    execution_id character varying(64) NOT NULL,
    resource_type character varying(32) NOT NULL,
    resource_name character varying(256) NOT NULL,
    namespace_id character varying(128) DEFAULT NULL::character varying,
    version character varying(64) DEFAULT NULL::character varying,
    status character varying(32) NOT NULL,
    pipeline text NOT NULL,
    create_time bigint NOT NULL,
    update_time bigint NOT NULL
);


--
-- Name: TABLE pipeline_execution; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.pipeline_execution IS 'AI资源发布审核Pipeline执行记录';


--
-- Name: COLUMN pipeline_execution.execution_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pipeline_execution.execution_id IS '执行ID';


--
-- Name: COLUMN pipeline_execution.resource_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pipeline_execution.resource_type IS '资源类型';


--
-- Name: COLUMN pipeline_execution.resource_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pipeline_execution.resource_name IS '资源名称';


--
-- Name: COLUMN pipeline_execution.namespace_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pipeline_execution.namespace_id IS '命名空间ID';


--
-- Name: COLUMN pipeline_execution.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pipeline_execution.version IS '版本';


--
-- Name: COLUMN pipeline_execution.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pipeline_execution.status IS '执行状态';


--
-- Name: COLUMN pipeline_execution.pipeline; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pipeline_execution.pipeline IS 'pipeline节点结果JSON';


--
-- Name: COLUMN pipeline_execution.create_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pipeline_execution.create_time IS '创建时间';


--
-- Name: COLUMN pipeline_execution.update_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pipeline_execution.update_time IS '修改时间';


--
-- Name: roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.roles (
    username character varying(50) NOT NULL,
    role character varying(50) NOT NULL
);


--
-- Name: tenant_capacity; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tenant_capacity (
    id bigint NOT NULL,
    tenant_id character varying(128) NOT NULL,
    quota integer NOT NULL,
    usage integer NOT NULL,
    max_size integer NOT NULL,
    max_aggr_count integer NOT NULL,
    max_aggr_size integer NOT NULL,
    max_history_count integer DEFAULT 0 NOT NULL,
    gmt_create timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE tenant_capacity; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.tenant_capacity IS '租户容量信息表';


--
-- Name: COLUMN tenant_capacity.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.tenant_capacity.id IS '主键ID';


--
-- Name: COLUMN tenant_capacity.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.tenant_capacity.tenant_id IS 'Tenant ID';


--
-- Name: COLUMN tenant_capacity.quota; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.tenant_capacity.quota IS '配额，0表示使用默认值';


--
-- Name: COLUMN tenant_capacity.usage; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.tenant_capacity.usage IS '使用量';


--
-- Name: COLUMN tenant_capacity.max_size; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.tenant_capacity.max_size IS '单个配置大小上限，单位为字节，0表示使用默认值';


--
-- Name: COLUMN tenant_capacity.max_aggr_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.tenant_capacity.max_aggr_count IS '聚合子配置最大个数';


--
-- Name: COLUMN tenant_capacity.max_aggr_size; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.tenant_capacity.max_aggr_size IS '单个聚合数据的子配置大小上限，单位为字节，0表示使用默认值';


--
-- Name: COLUMN tenant_capacity.max_history_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.tenant_capacity.max_history_count IS '最大变更历史数量';


--
-- Name: COLUMN tenant_capacity.gmt_create; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.tenant_capacity.gmt_create IS '创建时间';


--
-- Name: COLUMN tenant_capacity.gmt_modified; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.tenant_capacity.gmt_modified IS '修改时间';


--
-- Name: tenant_capacity_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tenant_capacity_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tenant_capacity_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tenant_capacity_id_seq OWNED BY public.tenant_capacity.id;


--
-- Name: tenant_info; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tenant_info (
    id bigint NOT NULL,
    kp character varying(128) NOT NULL,
    tenant_id character varying(128),
    tenant_name character varying(128),
    tenant_desc character varying(256),
    create_source character varying(32),
    gmt_create timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_modified timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE tenant_info; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.tenant_info IS 'tenant_info';


--
-- Name: COLUMN tenant_info.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.tenant_info.id IS 'id';


--
-- Name: COLUMN tenant_info.kp; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.tenant_info.kp IS 'kp';


--
-- Name: COLUMN tenant_info.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.tenant_info.tenant_id IS 'tenant_id';


--
-- Name: COLUMN tenant_info.tenant_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.tenant_info.tenant_name IS 'tenant_name';


--
-- Name: COLUMN tenant_info.tenant_desc; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.tenant_info.tenant_desc IS 'tenant_desc';


--
-- Name: COLUMN tenant_info.create_source; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.tenant_info.create_source IS 'create_source';


--
-- Name: COLUMN tenant_info.gmt_create; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.tenant_info.gmt_create IS '创建时间';


--
-- Name: COLUMN tenant_info.gmt_modified; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.tenant_info.gmt_modified IS '修改时间';


--
-- Name: tenant_info_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tenant_info_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tenant_info_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tenant_info_id_seq OWNED BY public.tenant_info.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    username character varying(50) NOT NULL,
    password character varying(500) NOT NULL,
    enabled boolean NOT NULL
);


--
-- Name: ai_resource id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ai_resource ALTER COLUMN id SET DEFAULT nextval('public.ai_resource_id_seq'::regclass);


--
-- Name: ai_resource_version id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ai_resource_version ALTER COLUMN id SET DEFAULT nextval('public.ai_resource_version_id_seq'::regclass);


--
-- Name: config_info id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_info ALTER COLUMN id SET DEFAULT nextval('public.config_info_id_seq'::regclass);


--
-- Name: config_info_gray id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_info_gray ALTER COLUMN id SET DEFAULT nextval('public.config_info_gray_id_seq'::regclass);


--
-- Name: config_tags_relation nid; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_tags_relation ALTER COLUMN nid SET DEFAULT nextval('public.config_tags_relation_nid_seq'::regclass);


--
-- Name: group_capacity id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.group_capacity ALTER COLUMN id SET DEFAULT nextval('public.group_capacity_id_seq'::regclass);


--
-- Name: his_config_info nid; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.his_config_info ALTER COLUMN nid SET DEFAULT nextval('public.his_config_info_nid_seq'::regclass);


--
-- Name: tenant_capacity id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tenant_capacity ALTER COLUMN id SET DEFAULT nextval('public.tenant_capacity_id_seq'::regclass);


--
-- Name: tenant_info id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tenant_info ALTER COLUMN id SET DEFAULT nextval('public.tenant_info_id_seq'::regclass);


--
-- Name: ai_resource ai_resource_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ai_resource
    ADD CONSTRAINT ai_resource_pkey PRIMARY KEY (id);


--
-- Name: ai_resource_version ai_resource_version_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ai_resource_version
    ADD CONSTRAINT ai_resource_version_pkey PRIMARY KEY (id);


--
-- Name: config_info_gray config_info_gray_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_info_gray
    ADD CONSTRAINT config_info_gray_pkey PRIMARY KEY (id);


--
-- Name: config_info config_info_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_info
    ADD CONSTRAINT config_info_pkey PRIMARY KEY (id);


--
-- Name: config_tags_relation config_tags_relation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_tags_relation
    ADD CONSTRAINT config_tags_relation_pkey PRIMARY KEY (nid);


--
-- Name: group_capacity group_capacity_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.group_capacity
    ADD CONSTRAINT group_capacity_pkey PRIMARY KEY (id);


--
-- Name: his_config_info his_config_info_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.his_config_info
    ADD CONSTRAINT his_config_info_pkey PRIMARY KEY (nid);


--
-- Name: pipeline_execution pipeline_execution_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pipeline_execution
    ADD CONSTRAINT pipeline_execution_pkey PRIMARY KEY (execution_id);


--
-- Name: tenant_capacity tenant_capacity_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tenant_capacity
    ADD CONSTRAINT tenant_capacity_pkey PRIMARY KEY (id);


--
-- Name: idx_ai_resource_gmt_modified; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ai_resource_gmt_modified ON public.ai_resource USING btree (gmt_modified);


--
-- Name: idx_ai_resource_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ai_resource_name ON public.ai_resource USING btree (name);


--
-- Name: idx_ai_resource_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ai_resource_type ON public.ai_resource USING btree (type);


--
-- Name: idx_ai_resource_ver_gmt_modified; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ai_resource_ver_gmt_modified ON public.ai_resource_version USING btree (gmt_modified);


--
-- Name: idx_ai_resource_ver_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ai_resource_ver_name ON public.ai_resource_version USING btree (name);


--
-- Name: idx_ai_resource_ver_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ai_resource_ver_status ON public.ai_resource_version USING btree (status);


--
-- Name: idx_dataid_gmt_modified_gray; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dataid_gmt_modified_gray ON public.config_info_gray USING btree (data_id, gmt_modified);


--
-- Name: idx_did; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_did ON public.his_config_info USING btree (data_id);


--
-- Name: idx_gmt_create; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gmt_create ON public.his_config_info USING btree (gmt_create);


--
-- Name: idx_gmt_modified; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gmt_modified ON public.his_config_info USING btree (gmt_modified);


--
-- Name: idx_gmt_modified_gray; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gmt_modified_gray ON public.config_info_gray USING btree (gmt_modified);


--
-- Name: idx_tenant_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tenant_id ON public.config_tags_relation USING btree (tenant_id);


--
-- Name: uk_ai_resource_ns_name_type; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_ai_resource_ns_name_type ON public.ai_resource USING btree (namespace_id, name, type, c_from);


--
-- Name: uk_ai_resource_ver_ns_name_type_ver; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_ai_resource_ver_ns_name_type_ver ON public.ai_resource_version USING btree (namespace_id, name, type, version);


--
-- Name: uk_configinfo_datagrouptenant; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_configinfo_datagrouptenant ON public.config_info USING btree (data_id, group_id, tenant_id);


--
-- Name: uk_configinfogray_datagrouptenantgray; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_configinfogray_datagrouptenantgray ON public.config_info_gray USING btree (data_id, group_id, tenant_id, gray_name);


--
-- Name: uk_configtagrelation_configidtag; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_configtagrelation_configidtag ON public.config_tags_relation USING btree (id, tag_name, tag_type);


--
-- Name: uk_group_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_group_id ON public.group_capacity USING btree (group_id);


--
-- Name: uk_role_permission; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_role_permission ON public.permissions USING btree (role, resource, action);


--
-- Name: uk_tenant_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_tenant_id ON public.tenant_capacity USING btree (tenant_id);


--
-- Name: uk_tenant_info_kptenantid; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_tenant_info_kptenantid ON public.tenant_info USING btree (kp, tenant_id);


--
-- Name: uk_username_role; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_username_role ON public.roles USING btree (username, role);


--
-- PostgreSQL database dump complete
--

\unrestrict 1aYnph3VbgdfjJV68WCH4UAJcZAwlS8sXkXFGiGZPcXOjm6FJdj0tUh4LnB0hLp

