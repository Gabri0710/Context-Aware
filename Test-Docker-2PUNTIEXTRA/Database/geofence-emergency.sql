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
    polygon public.geometry(Polygon,4326),
    title character varying
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
2	0101000020E610000004BDF69A31B6264018C255A4343E4640
1	0101000020E6100000D0F39947D3A92640C42BCC011B404640
\.


--
-- Data for Name: geofence-information; Type: TABLE DATA; Schema: emergency-schema; Owner: postgres
--

COPY "emergency-schema"."geofence-information" (id, polygon, title) FROM stdin;
-NhXnIzMNWHSl2q9cws1	0103000020E61000000100000008000000581779AB2CAD2640CC05D737504046400F63A48576AE26405C9C29C14240464080E126CCE5AE2640FC41CFC71B4046400F63A48576AE26409408D818F43F46406F81DE6F50AD26401076B875D83F464070FB0D254AAC2640C0FBF52EFB3F4640C880B26026AC2640F451D77432404640581779AB2CAD2640CC05D73750404640	Terremoto
-NhXnUcoIYNLaLk3ynPG	0103000020E6100000010000000600000089E77C91C3AA264084EFA916893F46406A66C78CF9AB2640B832B2B3713F46409A36073992AB26403C9C182E473F4640429C196C40AA26403C94AA96373F464082351375A1A9264064B126736D3F464089E77C91C3AA264084EFA916893F4640	Maremoto
-NhXnboj-Q6xbgYZdOa9	0103000020E61000000100000009000000E05642CF37B42640B8B888D94C3F4640D1208C958DB52640F43CAC64503F464081BED82F64B62640DCECC249273F4640292C011C70B626403810A5D7F23E4640C8836B0AE1B52640CCD6C706C93E464068BDE46B73B42640D822CCA5C23E4640788213EC8CB32640E8A7D70BEB3E4640005A3D4259B32640C80D4F9E213F4640E05642CF37B42640B8B888D94C3F4640	Attentato
-NhXperGJ3XLG74fZ-9l	0103000020E6100000010000000600000045705C989DB2264004B2876C9A3E4640EA4C1BD025B32640F4747253923E46400C8A4AFE39B326409C4B5A927A3E4640DD39BE1464B226405810620D783E46405204D6CA43B226400CD7DD2F8E3E464045705C989DB2264004B2876C9A3E4640	Bomba
\.


--
-- Data for Name: user-information; Type: TABLE DATA; Schema: emergency-schema; Owner: postgres
--

COPY "emergency-schema"."user-information" (username, posizione, activity) FROM stdin;
TyWovwILKjXtWCUx1hizU2G8pAw2	0101000020E61000005AE9FF6673AA2640822BEADD693F4640	CAR
Ckr3vMTPhEPscO19N9NRnKkbqiK2	0101000020E6100000273DC21F34AD2640CA32C4B12E404640	CAR
q3hgYoA1X5Q7cgS5wV09akV6q7z2	0101000020E6100000C4F7B479D2B42640E6F74729323F4640	WALKING
XSAcoDKDp9fFek2N84Usu1caTZw1	0101000020E6100000F61CA21BABB226407F4DD6A8873E4640	WALKING
2ZdWWtUoMufHd0Qz7dPGk96GPdz1	0101000020E610000040FB912232B42640B8CCE9B2983E4640	WALKING
WzNfJEV42tOf627aQVdWCO7CsNr2	0101000020E610000039FFC066CAAC2640B3FEE08D16404640	WALKING
QxoqJxYBPGNJyVTPqn3UcV1CWgg2	0101000020E61000001CC242F7F6B42640390202E7D63E4640	WALKING
lvyP2Njfc0TsRtdGgozEjG4m6To2	0101000020E6100000892991442FAB2640E8D9ACFA5C3F4640	WALKING
ZhnwDrnfqXXxd6eaClAcZ5q0t3C2	0101000020E6100000894D1A59E8AD264015C616821C404640	WALKING
PzyDnLxN2AWLnSFqR3oHo7mJlVD3	0101000020E6100000F0969AF342B4264002BC0512143F4640	WALKING
WVzLsHQi8mZIHC8FXYYxNkk8U4P2	0101000020E6100000633953324AB5264019B61C9E143F4640	WALKING
uxLTfdIJy3RySysduaoQrfG9Wrl1	0101000020E6100000AC7FC63B76AA2640DB08D517533F4640	CAR
EYAVkmUbtTQUNwEpSACIKEEl2bt1	0101000020E6100000E1DD808582B42640123A9E85F33E4640	WALKING
QXb8nnl91ZWJbq9X3VFF6ZG3FDm1	0101000020E61000007CC49E2C6BAD2640679B1BD313404640	WALKING
26d7OpMyG6NMYpj9nenCIOSn3g22	0101000020E61000008D73E5FDC9B52640E1FC3CFCEB3E4640	WALKING
q4Hz6wRxdfX8xSHj8kYZmZKaAUm1	0101000020E610000082C5E1CCAFAE2640A1BE654E973F4640	CAR
	0101000020E6100000D8DFC8F264B1264024FC4113173F4640	CAR
usQpFGhMtnR548dWb5j4vaxGAn82	0101000020E610000026080DB386AD264059FAD005F53F4640	CAR
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

