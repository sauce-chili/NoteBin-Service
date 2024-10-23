create table if not exists note(
    id bigint primary key generated always as identity unique,
    title varchar(128),
    note_text text not null,

    create_at timestamptz default current_timestamp
)