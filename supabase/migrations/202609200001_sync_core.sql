-- LightTodo account-owned sync schema. Run with the Supabase migration tooling.
create sequence if not exists public.sync_change_sequence;

create or replace function public.prepare_sync_row()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if tg_op = 'INSERT' then
    new.user_id := auth.uid();
    new.sync_version := 1;
  else
    new.user_id := old.user_id;
    new.created_at := old.created_at;
    new.sync_version := old.sync_version + 1;
  end if;
  new.sync_seq := nextval('public.sync_change_sequence');
  new.server_updated_at := now();
  return new;
end;
$$;

create table if not exists public.tasks (
  id uuid primary key,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  title text not null default '',
  note text not null default '',
  start_at bigint not null,
  duration_minutes integer not null default 0 check (duration_minutes between 0 and 525600),
  priority smallint not null default 3 check (priority between 0 and 3),
  done boolean not null default false,
  completed_at bigint,
  reminder_minutes integer[] not null default '{}',
  source_goal_id uuid,
  source_course_id uuid,
  generated_day bigint,
  created_at bigint not null,
  updated_at bigint not null,
  deleted_at bigint,
  device_id uuid,
  sync_version bigint not null default 0,
  sync_seq bigint not null default 0,
  server_updated_at timestamptz not null default now()
);

create table if not exists public.goals (
  id uuid primary key,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  title text not null default '',
  daily_title text not null default '',
  deadline bigint not null,
  daily_hour smallint not null check (daily_hour between 0 and 23),
  daily_minute smallint not null check (daily_minute between 0 and 59),
  auto_add boolean not null default true,
  created_at bigint not null,
  updated_at bigint not null,
  deleted_at bigint,
  device_id uuid,
  sync_version bigint not null default 0,
  sync_seq bigint not null default 0,
  server_updated_at timestamptz not null default now()
);

create table if not exists public.courses (
  id uuid primary key,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  name text not null default '',
  location text not null default '',
  teacher text not null default '',
  note text not null default '',
  weekday smallint not null check (weekday between 1 and 7),
  start_period smallint not null check (start_period between 1 and 13),
  end_period smallint not null check (end_period between start_period and 13),
  start_week smallint not null check (start_week >= 1),
  end_week smallint not null check (end_week >= start_week),
  color_index smallint not null default 0,
  created_at bigint not null,
  updated_at bigint not null,
  deleted_at bigint,
  device_id uuid,
  sync_version bigint not null default 0,
  sync_seq bigint not null default 0,
  server_updated_at timestamptz not null default now()
);

create index if not exists tasks_user_sync_idx on public.tasks(user_id, sync_seq);
create index if not exists goals_user_sync_idx on public.goals(user_id, sync_seq);
create index if not exists courses_user_sync_idx on public.courses(user_id, sync_seq);

drop trigger if exists tasks_prepare_sync on public.tasks;
create trigger tasks_prepare_sync before insert or update on public.tasks for each row execute function public.prepare_sync_row();
drop trigger if exists goals_prepare_sync on public.goals;
create trigger goals_prepare_sync before insert or update on public.goals for each row execute function public.prepare_sync_row();
drop trigger if exists courses_prepare_sync on public.courses;
create trigger courses_prepare_sync before insert or update on public.courses for each row execute function public.prepare_sync_row();

alter table public.tasks enable row level security;
alter table public.goals enable row level security;
alter table public.courses enable row level security;

create policy "tasks_owner_select" on public.tasks for select to authenticated using (user_id = auth.uid());
create policy "tasks_owner_insert" on public.tasks for insert to authenticated with check (user_id = auth.uid());
create policy "tasks_owner_update" on public.tasks for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "goals_owner_select" on public.goals for select to authenticated using (user_id = auth.uid());
create policy "goals_owner_insert" on public.goals for insert to authenticated with check (user_id = auth.uid());
create policy "goals_owner_update" on public.goals for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "courses_owner_select" on public.courses for select to authenticated using (user_id = auth.uid());
create policy "courses_owner_insert" on public.courses for insert to authenticated with check (user_id = auth.uid());
create policy "courses_owner_update" on public.courses for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

create or replace function public.pull_changes(p_after bigint default 0, p_limit integer default 500)
returns table(entity_type text, entity_id uuid, sync_version bigint, sync_seq bigint, deleted_at bigint, payload jsonb)
language sql
security invoker
set search_path = public
as $$
  select * from (
    select 'task'::text, id, sync_version, sync_seq, deleted_at, to_jsonb(t.*) from public.tasks t where sync_seq > p_after
    union all
    select 'goal'::text, id, sync_version, sync_seq, deleted_at, to_jsonb(g.*) from public.goals g where sync_seq > p_after
    union all
    select 'course'::text, id, sync_version, sync_seq, deleted_at, to_jsonb(c.*) from public.courses c where sync_seq > p_after
  ) changes
  order by sync_seq
  limit least(greatest(p_limit, 1), 1000);
$$;

revoke all on function public.pull_changes(bigint, integer) from public;
grant execute on function public.pull_changes(bigint, integer) to authenticated;
grant select, insert, update on public.tasks, public.goals, public.courses to authenticated;
