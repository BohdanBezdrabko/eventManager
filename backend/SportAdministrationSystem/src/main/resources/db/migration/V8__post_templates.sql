create table if not exists post_templates (
                                              id            bigserial primary key,
                                              code          varchar(64) not null unique,
    name          varchar(128) not null,
    title_tpl     text not null,
    body_tpl      text not null,
    offset_days   int  not null default 0,
    offset_hours  int  not null default 0,
    active        boolean not null default true,
    category      varchar(64),             -- nullable: applies to all if null
    audience      varchar(32) not null,    -- matches Audience enum (STRING)
    channel       varchar(32) not null,    -- matches Channel enum (STRING)
    created_at    timestamp not null default now(),
    updated_at    timestamp not null default now()
    );

create index if not exists idx_post_templates_category on post_templates(category);
create index if not exists idx_post_templates_active on post_templates(active);

create table if not exists event_template_overrides (
                                                        id          bigserial primary key,
                                                        event_id    bigint not null references events(id) on delete cascade,
    template_id bigint not null references post_templates(id) on delete cascade,
    title_tpl   text,
    body_tpl    text,
    active      boolean,                   -- nullable: no change if null
    created_at  timestamp not null default now(),
    updated_at  timestamp not null default now(),
    unique(event_id, template_id)
    );

-- Дефолтні шаблони (TELEGRAM; PUBLIC для T-30 і T-1, інші SUBSCRIBERS)
insert into post_templates(code,name,title_tpl,body_tpl,offset_days,offset_hours,active,category,audience,channel)
values
    ('T_MINUS_30','За 30 днів',
     '{event_name}: 30 днів до старту',
     'Старт {start_at} у {location}. Залишилось {days_left} днів.', -30,0,true,null,'PUBLIC','TELEGRAM'),
    ('T_MINUS_14','За 14 днів',
     '{event_name}: нагадування',
     'До події {days_left} днів. Локація: {location}.', -14,0,true,null,'SUBSCRIBERS','TELEGRAM'),
    ('T_MINUS_7','За 7 днів',
     '{event_name}: останній тиждень',
     'Старт {start_at}. Підготуйтеся. {days_left} днів.', -7,0,true,null,'SUBSCRIBERS','TELEGRAM'),
    ('T_MINUS_1','Завтра',
     '{event_name} вже завтра',
     'Початок о {start_at}. Місце: {location}.', -1,0,true,null,'PUBLIC','TELEGRAM'),
    ('T_0','Сьогодні',
     '{event_name} стартує сьогодні',
     'Зустрічаємось у {location} о {start_at}.', 0,0,true,null,'SUBSCRIBERS','TELEGRAM');
