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
-Ng-maeM9CvPNUGsWTRn	0103000020E61000000100000006000000E13030F8F6AE26400096AB54CC3F464043CD707B86AF2640C89FEF16C33F4640B0DC7A748EAF26401CB5E01F9E3F4640F886C78257AE2640C4B3E8B39C3F46407D40ACBD13AE26403C8A2545BB3F4640E13030F8F6AE26400096AB54CC3F4640
-Ng09gWdX5x_FrFk3LQE	0103000020E610000001000000060000000D05868799AF2640C40BE3244E3F4640029FC62B22B026409C9B54E8403F46408C87C3FA2DB02640F4B982412E3F46400D05868799AF2640D047FF1F233F4640FA2FA70F3BAF2640ACD8B277323F46400D05868799AF2640C40BE3244E3F4640
-Ng09lKXADUEjUgIbsCx	0103000020E61000000100000007000000FB2ABFEE64B22640789EE329C23F46402B075BFAC1B22640B4631D28B93F46401546F58FDCB226408C25183FA53F464072A7600974B2264030DAB07F973F46407A50B710FFB1264088DBACFCA93F4640813EF629DFB12640D0903FA3C23F4640FB2ABFEE64B22640789EE329C23F4640
\.


--
-- Data for Name: user-information; Type: TABLE DATA; Schema: emergency-schema; Owner: postgres
--

COPY "emergency-schema"."user-information" (username, posizione, activity) FROM stdin;
0	0101000020E6100000DB48ECEDA9AF2640947A294F333F4640	\N
1	0101000020E6100000DB48ECEDA9AF2640947A294F333F4640	CAR
2	0101000020E6100000DB48ECEDA9AF2640947A294F333F4640	CAR
user0	0101000020E6100000DB48ECEDA9AF2640947A294F333F4640	CAR
	0101000020E61000005BA09394AAAF26400BC336983C3F4640	CAR
USER-TEST2	0101000020E6100000227832EEBCAF2640884677103B3F4640	CAR
USER-TEST3	0101000020E610000006C71B4FBAAF26400BC336983C3F4640	CAR
USER-TEST4	0101000020E610000006C71B4FBAAF26400BC336983C3F4640	CAR
q4Hz6wRxdfX8xSHj8kYZmZKaAUm1	0101000020E610000014A7A498CDB12640C9B08A37323F4640	CAR
USER-TEST	0101000020E610000006C71B4FBAAF26400BC336983C3F4640	CAR
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

