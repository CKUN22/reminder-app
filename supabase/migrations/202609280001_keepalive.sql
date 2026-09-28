-- A harmless public read target for the scheduled project activity check.
create table if not exists public.project_keepalive (
    id integer primary key check (id = 1),
    message text not null default 'ok'
);
insert into public.project_keepalive (id) values (1) on conflict (id) do nothing;
alter table public.project_keepalive enable row level security;
create policy "Public keepalive read" on public.project_keepalive
    for select to anon using (id = 1);
grant select on public.project_keepalive to anon;
