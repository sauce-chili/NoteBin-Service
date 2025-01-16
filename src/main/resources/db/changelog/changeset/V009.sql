create table if not exists view_note(
    id bigint primary key generated always as identity unique,
    user_id bigint,
    note_id bigint NOT NULL,
    viewed_at timestamptz default current_timestamp
)