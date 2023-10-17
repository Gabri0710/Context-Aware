--
-- PostgreSQL database dump
--

-- Dumped from database version 15.2
-- Dumped by pg_dump version 15.2

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
-- Name: emergency-schema; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA "emergency-schema";


ALTER SCHEMA "emergency-schema" OWNER TO postgres;

--
-- Name: postgis; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;


--
-- Name: EXTENSION postgis; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION postgis IS 'PostGIS geometry and geography spatial types and functions';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: edge-information; Type: TABLE; Schema: emergency-schema; Owner: postgres
--

CREATE TABLE "emergency-schema"."edge-information" (
    id character varying(255) NOT NULL,
    posizione public.geometry(Point,4326)
);


ALTER TABLE "emergency-schema"."edge-information" OWNER TO postgres;

--
-- Name: user-information; Type: TABLE; Schema: emergency-schema; Owner: postgres
--

CREATE TABLE "emergency-schema"."user-information" (
    username character varying(255) NOT NULL,
    posizione public.geometry(Point,4326),
    activity character varying(7)
);


ALTER TABLE "emergency-schema"."user-information" OWNER TO postgres;

--
-- Name: geofance-coordinate_user_id_seq; Type: SEQUENCE; Schema: emergency-schema; Owner: postgres
--

CREATE SEQUENCE "emergency-schema"."geofance-coordinate_user_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE "emergency-schema"."geofance-coordinate_user_id_seq" OWNER TO postgres;

--
-- Name: geofance-coordinate_user_id_seq; Type: SEQUENCE OWNED BY; Schema: emergency-schema; Owner: postgres
--

ALTER SEQUENCE "emergency-schema"."geofance-coordinate_user_id_seq" OWNED BY "emergency-schema"."user-information".username;


--
-- Name: geofence-information; Type: TABLE; Schema: emergency-schema; Owner: postgres
--

CREATE TABLE "emergency-schema"."geofence-information" (
    id character varying(255) NOT NULL,
    polygon public.geometry(Polygon,4326)
);


ALTER TABLE "emergency-schema"."geofence-information" OWNER TO postgres;

--
-- Name: geofence_information_id_seq; Type: SEQUENCE; Schema: emergency-schema; Owner: postgres
--

CREATE SEQUENCE "emergency-schema".geofence_information_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE "emergency-schema".geofence_information_id_seq OWNER TO postgres;

--
-- Name: geofence_information_id_seq; Type: SEQUENCE OWNED BY; Schema: emergency-schema; Owner: postgres
--

ALTER SEQUENCE "emergency-schema".geofence_information_id_seq OWNED BY "emergency-schema"."geofence-information".id;


--
-- Name: user_credentials; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_credentials (
    username character varying(255) NOT NULL,
    password character varying(255)
);


ALTER TABLE public.user_credentials OWNER TO postgres;

--
-- Name: geofence-information id; Type: DEFAULT; Schema: emergency-schema; Owner: postgres
--

ALTER TABLE ONLY "emergency-schema"."geofence-information" ALTER COLUMN id SET DEFAULT nextval('"emergency-schema".geofence_information_id_seq'::regclass);


--
-- Name: user-information username; Type: DEFAULT; Schema: emergency-schema; Owner: postgres
--

ALTER TABLE ONLY "emergency-schema"."user-information" ALTER COLUMN username SET DEFAULT nextval('"emergency-schema"."geofance-coordinate_user_id_seq"'::regclass);


--
-- Data for Name: edge-information; Type: TABLE DATA; Schema: emergency-schema; Owner: postgres
--

COPY "emergency-schema"."edge-information" (id, posizione) FROM stdin;
1	0101000020E6100000F5E19DA21EAA26407800DAF614404640
2	0101000020E610000004BDF69A31B6264018C255A4343E4640
\.


--
-- Data for Name: geofence-information; Type: TABLE DATA; Schema: emergency-schema; Owner: postgres
--

COPY "emergency-schema"."geofence-information" (id, polygon) FROM stdin;
-Ngzq8oJBY1ADSFR49LW	0103000020E6100000010000000700000097A2739E71AA2640DC76E3F28B3F46401E48BFE983AB2640D4FA0B15793F4640AC4E23AB8FAB2640C0FE901B4E3F4640AD7BDA26B8AA2640F0F6AC65373F46407DE68192B3A92640FCA33DA0433F46405AE6EC8C84A926406CFB38F36E3F464097A2739E71AA2640DC76E3F28B3F4640
-NgzqEDRohIrWTP2kuUw	0103000020E61000000100000009000000BF96FC262BB4264058D2A31D463F4640FD3004B130B526402CB3C3D74A3F4640BD1E06113FB62640080C7A1F293F4640E698AD7634B62640ACF86B8FED3E4640FDF1608BEFB52640989E0886CB3E464081C3A1AC7AB42640A8E6BFC8CC3E46402E6CC66F7AB32640D8AD2BC4EF3E46407624258F53B32640A4DB2E622A3F4640BF96FC262BB4264058D2A31D463F4640
-NgzqIHJaBm4RmlQoP8W	0103000020E610000001000000070000002BC009672AAD264090B770C9494046409C73781861AE2640807F40733C40464014361BE736AE264068910FBC05404640C0F36EB897AD2640C873EFA3E73F464020D5B63FB9AC2640947F1E22FB3F46409D5BA7EA34AC26406C3C6A532A4046402BC009672AAD264090B770C949404640
-NgzqMlxaUp0orYpHuyb	0103000020E610000001000000060000002E06965E9AB22640D00437F79A3E464013606E6928B32640BC3FA6E78F3E4640F04AB364EAB226400C6889EC7B3E4640DA9F535848B22640F05CA434803E46407176A65418B22640306867D4933E46402E06965E9AB22640D00437F79A3E4640
-NgzqQ9vsj8itJ_ghaj4	0103000020E610000001000000060000009BC521800FB32640D4E2877A10404640770E1A3CC4B32640643C0A57F53F464026D81A87E5B326404CAE0B64C83F464043D8D11BA2B226408CAC80C8CB3F46408B578021F2B1264080C3EE7DF43F46409BC521800FB32640D4E2877A10404640
\.


--
-- Data for Name: user-information; Type: TABLE DATA; Schema: emergency-schema; Owner: postgres
--

COPY "emergency-schema"."user-information" (username, posizione, activity) FROM stdin;
usQpFGhMtnR548dWb5j4vaxGAn82	0101000020E61000003D61890794AD2640578C0464F93F4640	WALKING
q3hgYoA1X5Q7cgS5wV09akV6q7z2	0101000020E6100000C4F7B479D2B42640E6F74729323F4640	WALKING
XSAcoDKDp9fFek2N84Usu1caTZw1	0101000020E6100000F61CA21BABB226407F4DD6A8873E4640	WALKING
2ZdWWtUoMufHd0Qz7dPGk96GPdz1	0101000020E610000040FB912232B42640B8CCE9B2983E4640	WALKING
uxLTfdIJy3RySysduaoQrfG9Wrl1	0101000020E610000068DCE56C4BAA264048DC63E9433F4640	WALKING
WzNfJEV42tOf627aQVdWCO7CsNr2	0101000020E610000039FFC066CAAC2640B3FEE08D16404640	WALKING
QxoqJxYBPGNJyVTPqn3UcV1CWgg2	0101000020E61000001CC242F7F6B42640390202E7D63E4640	WALKING
lvyP2Njfc0TsRtdGgozEjG4m6To2	0101000020E6100000892991442FAB2640E8D9ACFA5C3F4640	WALKING
ZhnwDrnfqXXxd6eaClAcZ5q0t3C2	0101000020E6100000894D1A59E8AD264015C616821C404640	WALKING
PzyDnLxN2AWLnSFqR3oHo7mJlVD3	0101000020E6100000F0969AF342B4264002BC0512143F4640	WALKING
WVzLsHQi8mZIHC8FXYYxNkk8U4P2	0101000020E6100000633953324AB5264019B61C9E143F4640	WALKING
EYAVkmUbtTQUNwEpSACIKEEl2bt1	0101000020E6100000E1DD808582B42640123A9E85F33E4640	WALKING
QXb8nnl91ZWJbq9X3VFF6ZG3FDm1	0101000020E61000007CC49E2C6BAD2640679B1BD313404640	WALKING
TyWovwILKjXtWCUx1hizU2G8pAw2	0101000020E61000001D4421B880AA2640B3D2A414743F4640	WALKING
26d7OpMyG6NMYpj9nenCIOSn3g22	0101000020E61000008D73E5FDC9B52640E1FC3CFCEB3E4640	WALKING
q4Hz6wRxdfX8xSHj8kYZmZKaAUm1	0101000020E6100000E71DA7E848AE2640132C0E677E3F4640	WALKING
Ckr3vMTPhEPscO19N9NRnKkbqiK2	0101000020E610000046A5225F53AD26402C05EE8A2A404640	WALKING
\.


--
-- Data for Name: spatial_ref_sys; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.spatial_ref_sys (srid, auth_name, auth_srid, srtext, proj4text) FROM stdin;
\.


--
-- Data for Name: user_credentials; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_credentials (username, password) FROM stdin;
\.


--
-- Name: geofance-coordinate_user_id_seq; Type: SEQUENCE SET; Schema: emergency-schema; Owner: postgres
--

SELECT pg_catalog.setval('"emergency-schema"."geofance-coordinate_user_id_seq"', 1, false);


--
-- Name: geofence_information_id_seq; Type: SEQUENCE SET; Schema: emergency-schema; Owner: postgres
--

SELECT pg_catalog.setval('"emergency-schema".geofence_information_id_seq', 1, false);


--
-- Name: edge-information edge-information_pkey; Type: CONSTRAINT; Schema: emergency-schema; Owner: postgres
--

ALTER TABLE ONLY "emergency-schema"."edge-information"
    ADD CONSTRAINT "edge-information_pkey" PRIMARY KEY (id);


--
-- Name: user-information geofance-coordinate_pkey; Type: CONSTRAINT; Schema: emergency-schema; Owner: postgres
--

ALTER TABLE ONLY "emergency-schema"."user-information"
    ADD CONSTRAINT "geofance-coordinate_pkey" PRIMARY KEY (username);


--
-- Name: geofence-information geofence_information_pkey; Type: CONSTRAINT; Schema: emergency-schema; Owner: postgres
--

ALTER TABLE ONLY "emergency-schema"."geofence-information"
    ADD CONSTRAINT geofence_information_pkey PRIMARY KEY (id);


--
-- Name: user_credentials user_credentials_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_credentials
    ADD CONSTRAINT user_credentials_pkey PRIMARY KEY (username);


--
-- PostgreSQL database dump complete
--

