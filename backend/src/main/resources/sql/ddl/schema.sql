-- public.users definition

-- Drop table

-- DROP TABLE users;

CREATE TABLE users (
                       id bigserial NOT NULL,
                       email varchar(100) NOT NULL,
                       password_hash varchar(255) NOT NULL,
                       "name" varchar(50) NOT NULL,
                       is_active bool DEFAULT true NULL,
                       created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                       updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                       CONSTRAINT users_email_key UNIQUE (email),
                       CONSTRAINT users_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_users_active ON public.users USING btree (is_active);
CREATE INDEX idx_users_email ON public.users USING btree (email);


-- public.bots definition

-- Drop table

-- DROP TABLE bots;

CREATE TABLE bots (
                      id bigserial NOT NULL,
                      user_id int8 NOT NULL,
                      "name" varchar(100) NOT NULL,
                      description text NULL,
                      site_url varchar(255) NULL,
                      is_active bool DEFAULT true NULL,
                      created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                      updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                      CONSTRAINT bots_pkey PRIMARY KEY (id),
                      CONSTRAINT bots_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_bots_active ON public.bots USING btree (is_active);
CREATE INDEX idx_bots_site_url ON public.bots USING btree (site_url);
CREATE INDEX idx_bots_user_id ON public.bots USING btree (user_id);


-- public.file_uploads definition

-- Drop table

-- DROP TABLE file_uploads;

CREATE TABLE file_uploads (
                              id bigserial NOT NULL,
                              user_id int8 NULL,
                              original_filename varchar(255) NOT NULL,
                              stored_filename varchar(255) NOT NULL,
                              file_path varchar(500) NOT NULL,
                              content_type varchar(100) NOT NULL,
                              file_size int8 NOT NULL,
                              file_hash varchar(64) NOT NULL,
                              status varchar(20) DEFAULT 'ACTIVE'::character varying NULL,
                              created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                              updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                              CONSTRAINT file_uploads_pkey PRIMARY KEY (id),
                              CONSTRAINT file_uploads_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'DELETED'::character varying, 'QUARANTINE'::character varying])::text[]))),
	CONSTRAINT file_uploads_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX idx_file_uploads_hash ON public.file_uploads USING btree (file_hash);
CREATE INDEX idx_file_uploads_status ON public.file_uploads USING btree (status);
CREATE INDEX idx_file_uploads_user_id ON public.file_uploads USING btree (user_id);


-- public.bot_options definition

-- Drop table

-- DROP TABLE bot_options;

CREATE TABLE bot_options (
                             id bigserial NOT NULL,
                             bot_id int8 NOT NULL,
                             option_key varchar(50) NOT NULL,
                             option_value text NOT NULL,
                             value_type varchar(20) DEFAULT 'STRING'::character varying NULL,
                             created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                             updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                             CONSTRAINT bot_options_pkey PRIMARY KEY (id),
                             CONSTRAINT bot_options_value_type_check CHECK (((value_type)::text = ANY ((ARRAY['STRING'::character varying, 'NUMBER'::character varying, 'BOOLEAN'::character varying, 'JSON'::character varying])::text[]))),
	CONSTRAINT bot_options_bot_id_fkey FOREIGN KEY (bot_id) REFERENCES bots(id) ON DELETE CASCADE
);
CREATE INDEX idx_bot_options_bot_id ON public.bot_options USING btree (bot_id);
CREATE UNIQUE INDEX idx_bot_options_key ON public.bot_options USING btree (bot_id, option_key);


-- public.file_links definition

-- Drop table

-- DROP TABLE file_links;

CREATE TABLE file_links (
                            id bigserial NOT NULL,
                            file_id int8 NOT NULL,
                            link_token varchar(100) NOT NULL,
                            access_type varchar(20) DEFAULT 'PUBLIC'::character varying NULL,
                            expires_at timestamp NULL,
                            is_active bool DEFAULT true NULL,
                            created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                            CONSTRAINT file_links_access_type_check CHECK (((access_type)::text = ANY ((ARRAY['PUBLIC'::character varying, 'PRIVATE'::character varying, 'TEMPORARY'::character varying])::text[]))),
	CONSTRAINT file_links_link_token_key UNIQUE (link_token),
	CONSTRAINT file_links_pkey PRIMARY KEY (id),
	CONSTRAINT file_links_file_id_fkey FOREIGN KEY (file_id) REFERENCES file_uploads(id) ON DELETE CASCADE
);
CREATE INDEX idx_file_links_active ON public.file_links USING btree (is_active);
CREATE INDEX idx_file_links_expires ON public.file_links USING btree (expires_at);
CREATE INDEX idx_file_links_file_id ON public.file_links USING btree (file_id);
CREATE INDEX idx_file_links_token ON public.file_links USING btree (link_token);


-- public.conversations definition

-- Drop table

-- DROP TABLE conversations;

CREATE TABLE conversations (
                               id bigserial NOT NULL,
                               user_id int8 NULL,
                               bot_id int8 NOT NULL,
                               current_scenario_id int8 NULL,
                               current_step_id int8 NULL,
                               context_data jsonb DEFAULT '{}'::jsonb NULL,
                               status varchar(20) DEFAULT 'ACTIVE'::character varying NULL,
                               session_id varchar(100) NULL,
                               started_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                               last_message_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                               ended_at timestamp NULL,
                               CONSTRAINT conversations_pkey PRIMARY KEY (id),
                               CONSTRAINT conversations_session_id_key UNIQUE (session_id),
                               CONSTRAINT conversations_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'PAUSED'::character varying, 'COMPLETED'::character varying, 'ABANDONED'::character varying])::text[])))
);
CREATE INDEX idx_conversations_bot_id ON public.conversations USING btree (bot_id);
CREATE INDEX idx_conversations_last_message ON public.conversations USING btree (last_message_at);
CREATE INDEX idx_conversations_session_id ON public.conversations USING btree (session_id);
CREATE INDEX idx_conversations_status ON public.conversations USING btree (status);
CREATE INDEX idx_conversations_user_id ON public.conversations USING btree (user_id);


-- public.messages definition

-- Drop table

-- DROP TABLE messages;

CREATE TABLE messages (
                          id bigserial NOT NULL,
                          conversation_id int8 NOT NULL,
                          sender_type varchar(10) NOT NULL,
                          "content" text NOT NULL,
                          message_type varchar(20) DEFAULT 'TEXT'::character varying NULL,
                          metadata jsonb DEFAULT '{}'::jsonb NULL,
                          is_read bool DEFAULT false NULL,
                          created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                          CONSTRAINT messages_message_type_check CHECK (((message_type)::text = ANY ((ARRAY['TEXT'::character varying, 'IMAGE'::character varying, 'FILE'::character varying, 'QUICK_REPLY'::character varying, 'CARD'::character varying])::text[]))),
	CONSTRAINT messages_pkey PRIMARY KEY (id),
	CONSTRAINT messages_sender_type_check CHECK (((sender_type)::text = ANY ((ARRAY['USER'::character varying, 'BOT'::character varying, 'SYSTEM'::character varying])::text[])))
);
CREATE INDEX idx_messages_conversation_id ON public.messages USING btree (conversation_id);
CREATE INDEX idx_messages_created_at ON public.messages USING btree (created_at);
CREATE INDEX idx_messages_sender_type ON public.messages USING btree (sender_type);
CREATE INDEX idx_messages_unread ON public.messages USING btree (is_read) WHERE (is_read = false);


-- public.scenario_steps definition

-- Drop table

-- DROP TABLE scenario_steps;

CREATE TABLE scenario_steps (
                                id bigserial NOT NULL,
                                scenario_id int8 NOT NULL,
                                step_type varchar(20) NOT NULL,
                                "content" text NOT NULL,
                                input_type varchar(20) NULL,
                                conditions jsonb NULL,
                                next_step_id int8 NULL,
                                is_start_step bool DEFAULT false NULL,
                                order_index int4 NOT NULL,
                                created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                                updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                                CONSTRAINT scenario_steps_input_type_check CHECK (((input_type)::text = ANY ((ARRAY['TEXT'::character varying, 'NUMBER'::character varying, 'EMAIL'::character varying, 'PHONE'::character varying, 'YES_NO'::character varying, 'CHOICE'::character varying])::text[]))),
	CONSTRAINT scenario_steps_pkey PRIMARY KEY (id),
	CONSTRAINT scenario_steps_step_type_check CHECK (((step_type)::text = ANY ((ARRAY['MESSAGE'::character varying, 'QUESTION'::character varying, 'CONDITION'::character varying, 'ACTION'::character varying])::text[])))
);
CREATE INDEX idx_scenario_steps_next_step ON public.scenario_steps USING btree (next_step_id);
CREATE INDEX idx_scenario_steps_order ON public.scenario_steps USING btree (scenario_id, order_index);
CREATE INDEX idx_scenario_steps_scenario_id ON public.scenario_steps USING btree (scenario_id);


-- public.scenarios definition

-- Drop table

-- DROP TABLE scenarios;

CREATE TABLE scenarios (
                           id bigserial NOT NULL,
                           bot_id int8 NOT NULL,
                           "name" varchar(100) NOT NULL,
                           description text NULL,
                           start_step_id int8 NULL,
                           is_default bool DEFAULT false NULL,
                           created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                           updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                           CONSTRAINT scenarios_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_scenarios_bot_id ON public.scenarios USING btree (bot_id);
CREATE INDEX idx_scenarios_default ON public.scenarios USING btree (is_default);


-- public.conversations foreign keys

ALTER TABLE public.conversations ADD CONSTRAINT conversations_bot_id_fkey FOREIGN KEY (bot_id) REFERENCES bots(id) ON DELETE CASCADE;
ALTER TABLE public.conversations ADD CONSTRAINT conversations_current_scenario_id_fkey FOREIGN KEY (current_scenario_id) REFERENCES scenarios(id) ON DELETE SET NULL;
ALTER TABLE public.conversations ADD CONSTRAINT conversations_current_step_id_fkey FOREIGN KEY (current_step_id) REFERENCES scenario_steps(id) ON DELETE SET NULL;
ALTER TABLE public.conversations ADD CONSTRAINT conversations_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;


-- public.messages foreign keys

ALTER TABLE public.messages ADD CONSTRAINT messages_conversation_id_fkey FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE;


-- public.scenario_steps foreign keys

ALTER TABLE public.scenario_steps ADD CONSTRAINT scenario_steps_next_step_id_fkey FOREIGN KEY (next_step_id) REFERENCES scenario_steps(id) ON DELETE SET NULL;
ALTER TABLE public.scenario_steps ADD CONSTRAINT scenario_steps_scenario_id_fkey FOREIGN KEY (scenario_id) REFERENCES scenarios(id) ON DELETE CASCADE;


-- public.scenarios foreign keys

ALTER TABLE public.scenarios ADD CONSTRAINT scenarios_bot_id_fkey FOREIGN KEY (bot_id) REFERENCES bots(id) ON DELETE CASCADE;
ALTER TABLE public.scenarios ADD CONSTRAINT scenarios_start_step_id_fkey FOREIGN KEY (start_step_id) REFERENCES scenario_steps(id);