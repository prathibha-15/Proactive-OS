import { useEffect, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, Navigate, Route, Routes, useNavigate, useParams } from 'react-router-dom'
import { journalApi, type Journal } from './api/journalApi'
import { eventsApi, LIFE_EVENT_TYPES, type LifeEvent, type LifeEventRequest, type LifeEventType } from './api/eventsApi'
import { getProactiveInsights } from './api/insightsApi'

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'long' }).format(new Date(`${value}T00:00:00`))
}

function formatTimestamp(value: string | null) {
  if (!value) return 'Time unknown'
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function localDateKey(value: Date) {
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function eventDateKey(value: string | null) {
  return value ? localDateKey(new Date(value)) : null
}

function Layout({ children }: { children: React.ReactNode }) {
  return <main className="min-h-screen bg-stone-50 px-5 py-8 text-stone-950 sm:px-10 sm:py-10"><div className="mx-auto max-w-4xl"><header className="flex flex-wrap items-center justify-between gap-4 border-b border-stone-300 pb-5"><Link to="/dashboard" className="text-sm font-bold tracking-[0.16em] text-teal-700">PROACTIVE OS</Link><nav aria-label="Main navigation" className="flex flex-wrap gap-x-5 gap-y-2"><Link to="/dashboard" className="text-sm font-medium text-stone-600 hover:text-teal-700">Overview</Link><Link to="/" className="text-sm font-medium text-stone-600 hover:text-teal-700">Write journal</Link><Link to="/journals" className="text-sm font-medium text-stone-600 hover:text-teal-700">Journals</Link><Link to="/events" className="text-sm font-medium text-stone-600 hover:text-teal-700">Life events</Link></nav></header>{children}</div></main>
}

function JournalForm({ initialContent = '', submitLabel, onSubmit, isPending, error }: { initialContent?: string, submitLabel: string, onSubmit: (content: string) => void, isPending: boolean, error?: string }) {
  const [content, setContent] = useState(initialContent)

  useEffect(() => setContent(initialContent), [initialContent])

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    onSubmit(content)
  }

  return <form onSubmit={submit} className="mt-10"><label htmlFor="content" className="text-3xl font-semibold sm:text-4xl">How was your day?</label><p className="mt-3 text-stone-600">Write freely. Your words stay exactly as you enter them.</p><textarea id="content" value={content} onChange={(event) => setContent(event.target.value)} placeholder="Tell Proactive OS what you did today..." className="mt-8 min-h-64 w-full resize-y border border-stone-300 bg-white p-5 text-lg leading-8 outline-none transition focus:border-teal-700 focus:ring-1 focus:ring-teal-700" disabled={isPending} /><div className="mt-5 flex items-center gap-4"><button type="submit" disabled={isPending || !content.trim()} className="bg-teal-700 px-5 py-3 font-semibold text-white transition hover:bg-teal-800 disabled:cursor-not-allowed disabled:bg-stone-300">{isPending ? 'Saving...' : submitLabel}</button>{error && <p role="alert" className="text-sm text-rose-700">{error}</p>}</div></form>
}

function ComposePage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const createMutation = useMutation({ mutationFn: journalApi.createJournal, onSuccess: (journal) => { queryClient.invalidateQueries({ queryKey: ['journals'] }); navigate(`/journals/${journal.id}`) } })

  return <Layout><JournalForm submitLabel="Save journal" isPending={createMutation.isPending} error={createMutation.error?.message} onSubmit={(content) => createMutation.mutate({ content })} /></Layout>
}

function DashboardPage() {
  const today = localDateKey(new Date())
  const journalsQuery = useQuery({ queryKey: ['journals'], queryFn: journalApi.getJournals })
  const eventsQuery = useQuery({ queryKey: ['events'], queryFn: () => eventsApi.getEvents() })
  const insightsQuery = useQuery({ queryKey: ['insights'], queryFn: getProactiveInsights })
  const journals = journalsQuery.data ?? []
  const events = eventsQuery.data ?? []
  const todayJournals = journals.filter((journal) => journal.entryDate === today)
  const recentEvents = [...events]
    .sort((left, right) => new Date(right.eventTime ?? right.createdAt).getTime() - new Date(left.eventTime ?? left.createdAt).getTime())
    .slice(0, 5)

  return <Layout><section className="py-10">
    <p className="text-sm font-semibold tracking-[0.14em] text-teal-700">PROACTIVE OS / OVERVIEW</p>
    <div className="mt-3 flex flex-wrap items-end justify-between gap-4">
      <div><h1 className="text-4xl font-semibold sm:text-5xl">Your activity</h1><p className="mt-2 text-stone-600">A view of what you have recorded.</p></div>
      <Link to="/" className="bg-teal-700 px-4 py-3 font-semibold text-white transition hover:bg-teal-800">Write a journal</Link>
    </div>
    {(journalsQuery.isError || eventsQuery.isError || insightsQuery.isError) && <p role="alert" className="mt-6 text-rose-700">{journalsQuery.error?.message ?? eventsQuery.error?.message ?? insightsQuery.error?.message}</p>}
    {(journalsQuery.isPending || eventsQuery.isPending || insightsQuery.isPending) && <p className="mt-8 text-stone-600">Loading your activity...</p>}
    {!journalsQuery.isPending && !eventsQuery.isPending && !insightsQuery.isPending && !journalsQuery.isError && !eventsQuery.isError && !insightsQuery.isError && insightsQuery.data && <>
      <div className="mt-8 grid gap-px border border-stone-300 bg-stone-300 sm:grid-cols-3">
        <div className="bg-white p-5"><p className="text-sm text-stone-500">Journals today</p><p className="mt-2 text-3xl font-semibold">{todayJournals.length}</p></div>
        <div className="bg-white p-5"><p className="text-sm text-stone-500">Events with known time, last 28 days</p><p className="mt-2 text-3xl font-semibold">{insightsQuery.data.knownTimeEventCount}</p></div>
        <div className="bg-white p-5"><p className="text-sm text-stone-500">Events with unknown time, all time</p><p className="mt-2 text-3xl font-semibold">{insightsQuery.data.unknownTimeEventCount}</p></div>
      </div>

      <section className="mt-10">
        <div className="flex items-baseline justify-between gap-4"><h2 className="text-2xl font-semibold">Recorded activity by type</h2><Link to="/events" className="text-sm font-medium text-teal-700 underline">View timeline</Link></div>
        <p className="mt-1 text-sm text-stone-500">Known event times, {insightsQuery.data.windowStart} through {insightsQuery.data.windowEnd} (UTC).</p>
        {insightsQuery.data.knownTimeEventCount === 0 ? <p className="mt-4 border-y border-stone-300 py-6 text-stone-600">No dated events in this window. Untimed entries are not assigned an activity date.</p> : <ul className="mt-4 grid gap-x-8 sm:grid-cols-2">{insightsQuery.data.eventCounts.filter(({ count }) => count > 0).map(({ type, count }) => <li key={type} className="flex justify-between border-b border-stone-300 py-3"><span>{type}</span><span className="font-semibold">{count}</span></li>)}</ul>}
      </section>

      <section className="mt-10">
        <div className="flex items-baseline justify-between gap-4"><h2 className="text-2xl font-semibold">Repeated activity observed</h2><span className="text-sm text-stone-500">Last 28 days</span></div>
        <p className="mt-1 text-sm text-stone-500">Repeated labels recorded on at least 3 distinct dated days. These are observations, not assumed habits.</p>
        {insightsQuery.data.repeatedActivities.length === 0 ? <p className="mt-4 text-stone-600">Not enough repeated, dated study or workout entries to report a pattern yet.</p> : <ul className="mt-3">{insightsQuery.data.repeatedActivities.map((observation) => <li key={`${observation.type}:${observation.label}`} className="flex flex-wrap justify-between gap-2 border-b border-stone-300 py-3"><span><strong>{observation.type}</strong> · {observation.label}</span><span className="text-stone-600">Recorded on {observation.distinctDays} distinct days ({observation.eventCount} entries)</span></li>)}</ul>}
      </section>

      <section className="mt-10 border-t border-stone-300 pt-8" aria-labelledby="recommendations-heading">
        <h2 id="recommendations-heading" className="text-2xl font-semibold">Recommendations</h2>
        <p className="mt-1 text-sm text-stone-500">Generated deterministically from recorded event data. These are observations and optional suggestions, not claims about habits or causes.</p>
        {insightsQuery.data.recommendations.length === 0
          ? <p className="mt-4 text-stone-600">There is not enough repeated or untimed event data for a recommendation yet.</p>
          : <ul className="mt-4 divide-y divide-stone-300">{insightsQuery.data.recommendations.map((recommendation) => <li key={recommendation.id} className="py-4"><p className="text-xs font-semibold uppercase tracking-wide text-teal-700">{recommendation.category.replace('_', ' ')}</p><h3 className="mt-1 text-lg font-semibold">{recommendation.title}</h3><p className="mt-1 text-stone-700">{recommendation.message}</p></li>)}</ul>}
      </section>

      <section className="mt-10">
        <div className="flex items-baseline justify-between gap-4"><h2 className="text-2xl font-semibold">Recent activity</h2><Link to="/journals" className="text-sm font-medium text-teal-700 underline">Journal history</Link></div>
        {recentEvents.length === 0 ? <p className="mt-4 text-stone-600">Events you extract or add will appear here.</p> : <ul className="mt-3">{recentEvents.map((event) => <li key={event.id} className="flex flex-wrap items-start justify-between gap-x-4 gap-y-1 border-b border-stone-300 py-4"><div><p className="font-semibold text-teal-700">{event.type}</p><p className="mt-1 text-stone-700">{eventSummary(event)}</p><p className="mt-1 text-xs uppercase tracking-wide text-stone-400">Source: {event.source}</p></div><p className="text-sm text-stone-500">{event.eventTime ? formatTimestamp(event.eventTime) : `Activity time unknown · recorded ${formatTimestamp(event.createdAt)}`}</p></li>)}</ul>}
      </section>
    </>}
  </section></Layout>
}

function JournalHistoryPage() {
  const journalsQuery = useQuery({ queryKey: ['journals'], queryFn: journalApi.getJournals })

  return <Layout><section className="py-10"><p className="text-sm font-semibold tracking-[0.14em] text-teal-700">YOUR JOURNALS</p><h1 className="mt-3 text-4xl font-semibold sm:text-5xl">History</h1>{journalsQuery.isPending && <p className="mt-10 text-stone-600">Loading journals...</p>}{journalsQuery.isError && <p role="alert" className="mt-10 text-rose-700">{journalsQuery.error.message}</p>}{journalsQuery.data?.length === 0 && <div className="mt-10 border-y border-stone-300 py-10"><p className="text-xl font-semibold">Your journal is ready when you are.</p><Link className="mt-4 inline-block text-teal-700 underline" to="/">Write your first entry</Link></div>}<div className="mt-8 divide-y divide-stone-300">{journalsQuery.data?.map((journal) => <JournalListItem key={journal.id} journal={journal} />)}</div></section></Layout>
}

function JournalListItem({ journal }: { journal: Journal }) {
  const preview = journal.content.length > 150 ? `${journal.content.slice(0, 150)}...` : journal.content
  return <Link to={`/journals/${journal.id}`} className="block py-6 transition hover:bg-white"><p className="font-semibold text-teal-700">{formatDate(journal.entryDate)}</p><p className="mt-2 max-w-2xl whitespace-pre-wrap text-lg leading-7">{preview}</p><p className="mt-3 text-sm text-stone-500">Updated {formatTimestamp(journal.updatedAt)}</p></Link>
}

function JournalDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const journalQuery = useQuery({ queryKey: ['journals', id], queryFn: () => journalApi.getJournal(id!), enabled: Boolean(id), retry: false })
  const updateMutation = useMutation({ mutationFn: (content: string) => journalApi.updateJournal(Number(id), { content }), onSuccess: (journal) => { queryClient.setQueryData(['journals', id], journal); queryClient.invalidateQueries({ queryKey: ['journals'] }); setEditing(false) } })
  const deleteMutation = useMutation({ mutationFn: () => journalApi.deleteJournal(Number(id)), onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['journals'] }); navigate('/journals') } })
  const extractMutation = useMutation({ mutationFn: () => eventsApi.extractJournalEvents(Number(id)), onSuccess: (events) => { queryClient.setQueryData(['journals', Number(id), 'events'], events); queryClient.invalidateQueries({ queryKey: ['events'] }) } })
  const [editing, setEditing] = useState(false)
  const [confirmingDelete, setConfirmingDelete] = useState(false)

  if (journalQuery.isPending) return <Layout><p className="py-10 text-stone-600">Loading journal...</p></Layout>
  if (journalQuery.isError) return <Layout><p role="alert" className="py-10 text-rose-700">{journalQuery.error.message}</p><Link className="text-teal-700 underline" to="/journals">Back to history</Link></Layout>
  const journal = journalQuery.data

  return <Layout><article className="py-10"><Link to="/journals" className="text-sm font-medium text-teal-700 hover:underline">Back to history</Link><p className="mt-8 text-sm font-semibold tracking-[0.14em] text-teal-700">{formatDate(journal.entryDate)}</p>{editing ? <JournalForm initialContent={journal.content} submitLabel="Save changes" isPending={updateMutation.isPending} error={updateMutation.error?.message} onSubmit={(content) => updateMutation.mutate(content)} /> : <><div className="mt-6 whitespace-pre-wrap text-xl leading-9 text-stone-800">{journal.content}</div><p className="mt-8 text-sm text-stone-500">Created {formatTimestamp(journal.createdAt)}{journal.updatedAt !== journal.createdAt && ` | Updated ${formatTimestamp(journal.updatedAt)}`}</p><div className="mt-8 flex flex-wrap gap-3"><button onClick={() => setEditing(true)} className="border border-stone-400 px-4 py-2 font-medium hover:border-teal-700">Edit</button><button onClick={() => setConfirmingDelete(true)} className="border border-rose-300 px-4 py-2 font-medium text-rose-700 hover:bg-rose-50">Delete</button></div></>}{confirmingDelete && <div role="dialog" aria-modal="true" className="mt-8 border-l-4 border-rose-600 bg-rose-50 p-5"><p className="font-semibold">Delete this journal entry?</p><p className="mt-1 text-sm text-stone-600">This cannot be undone.</p><div className="mt-4 flex gap-3"><button onClick={() => deleteMutation.mutate()} disabled={deleteMutation.isPending} className="bg-rose-700 px-4 py-2 font-medium text-white disabled:bg-stone-300">{deleteMutation.isPending ? 'Deleting...' : 'Delete entry'}</button><button onClick={() => setConfirmingDelete(false)} disabled={deleteMutation.isPending} className="px-4 py-2 font-medium">Cancel</button></div>{deleteMutation.isError && <p role="alert" className="mt-3 text-sm text-rose-700">{deleteMutation.error.message}</p>}</div>}<JournalEventsSection journalId={Number(id)} onExtract={() => extractMutation.mutate()} isExtracting={extractMutation.isPending} extractionError={extractMutation.error?.message} /></article></Layout>
}

type EventFieldConfig = { name: keyof LifeEventRequest, label: string, kind: 'text' | 'number' | 'select', options?: string[] }

const EVENT_TYPE_FIELDS: Record<LifeEventType, EventFieldConfig[]> = {
  SLEEP: [{ name: 'durationMinutes', label: 'Duration (minutes)', kind: 'number' }, { name: 'notes', label: 'Notes', kind: 'text' }],
  WATER: [{ name: 'quantity', label: 'Quantity', kind: 'number' }, { name: 'unit', label: 'Unit', kind: 'select', options: ['GLASS', 'ML', 'LITER'] }],
  FOOD: [{ name: 'description', label: 'Description', kind: 'text' }, { name: 'calories', label: 'Calories', kind: 'number' }],
  STUDY: [{ name: 'subject', label: 'Subject', kind: 'text' }, { name: 'durationMinutes', label: 'Duration (minutes)', kind: 'number' }],
  WORKOUT: [{ name: 'activityType', label: 'Activity type', kind: 'text' }, { name: 'durationMinutes', label: 'Duration (minutes)', kind: 'number' }],
  STEPS: [{ name: 'count', label: 'Step count', kind: 'number' }],
  JOB_APPLICATION: [{ name: 'company', label: 'Company', kind: 'text' }, { name: 'role', label: 'Role', kind: 'text' }, { name: 'status', label: 'Status', kind: 'text' }, { name: 'applicationCount', label: 'Applications', kind: 'number' }],
  MOOD: [{ name: 'mood', label: 'Mood', kind: 'text' }, { name: 'notes', label: 'Notes', kind: 'text' }],
}

function eventSummary(event: LifeEvent) {
  const fields = EVENT_TYPE_FIELDS[event.type]
  const parts = fields
    .map((field) => [field.label, event[field.name as keyof LifeEvent]] as const)
    .filter(([, value]) => value !== null && value !== undefined && value !== '')
    .map(([label, value]) => `${label}: ${value}`)
  return parts.length > 0 ? parts.join(' | ') : 'No details recorded'
}

function EventForm({ initialEvent, journalEntryId, submitLabel, onSubmit, isPending, error }: { initialEvent?: LifeEvent, journalEntryId?: number, submitLabel: string, onSubmit: (request: LifeEventRequest) => void, isPending: boolean, error?: string }) {
  const [type, setType] = useState<LifeEventType>(initialEvent?.type ?? 'STUDY')
  const [values, setValues] = useState<Record<string, string>>({})

  useEffect(() => {
    if (!initialEvent) return
    setType(initialEvent.type)
    const next: Record<string, string> = {}
    for (const field of EVENT_TYPE_FIELDS[initialEvent.type]) {
      const value = initialEvent[field.name as keyof LifeEvent]
      next[field.name] = value === null || value === undefined ? '' : String(value)
    }
    setValues(next)
  }, [initialEvent])

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const fields = EVENT_TYPE_FIELDS[type]
    const request: LifeEventRequest = { type, source: 'MANUAL', journalEntryId: journalEntryId ?? initialEvent?.journalEntryId ?? undefined }
    for (const field of fields) {
      const raw = values[field.name]
      if (raw === undefined || raw === '') continue
      ;(request as Record<string, unknown>)[field.name] = field.kind === 'number' ? Number(raw) : raw
    }
    onSubmit(request)
  }

  return <form onSubmit={submit} className="mt-6 space-y-4 border border-stone-300 bg-white p-5">
    <div>
      <label className="text-sm font-semibold text-stone-700">Event type</label>
      <select value={type} disabled={Boolean(initialEvent) || isPending} onChange={(event) => { setType(event.target.value as LifeEventType); setValues({}) }} className="mt-1 block w-full border border-stone-300 p-2">
        {LIFE_EVENT_TYPES.map((option) => <option key={option} value={option}>{option}</option>)}
      </select>
    </div>
    {EVENT_TYPE_FIELDS[type].map((field) => <div key={field.name}>
      <label className="text-sm font-semibold text-stone-700">{field.label}</label>
      {field.kind === 'select'
        ? <select value={values[field.name] ?? ''} disabled={isPending} onChange={(event) => setValues((current) => ({ ...current, [field.name]: event.target.value }))} className="mt-1 block w-full border border-stone-300 p-2">
            <option value="">Unknown</option>
            {field.options?.map((option) => <option key={option} value={option}>{option}</option>)}
          </select>
        : <input type={field.kind} value={values[field.name] ?? ''} disabled={isPending} onChange={(event) => setValues((current) => ({ ...current, [field.name]: event.target.value }))} placeholder="Leave blank if unknown" className="mt-1 block w-full border border-stone-300 p-2" />}
    </div>)}
    <div className="flex items-center gap-4">
      <button type="submit" disabled={isPending} className="bg-teal-700 px-4 py-2 font-semibold text-white transition hover:bg-teal-800 disabled:cursor-not-allowed disabled:bg-stone-300">{isPending ? 'Saving...' : submitLabel}</button>
      {error && <p role="alert" className="text-sm text-rose-700">{error}</p>}
    </div>
  </form>
}

function EventListItem({ event, onEdit, onDelete }: { event: LifeEvent, onEdit: () => void, onDelete: () => void }) {
  return <li className="border-b border-stone-300 py-4">
    <div className="flex items-center justify-between">
      <p className="font-semibold text-teal-700">{event.type}</p>
      <p className="text-sm text-stone-500">{formatTimestamp(event.eventTime)}</p>
    </div>
    <p className="mt-1 text-stone-700">{eventSummary(event)}</p>
    <p className="mt-1 text-xs uppercase tracking-wide text-stone-400">Source: {event.source}</p>
    <div className="mt-2 flex gap-3">
      <button onClick={onEdit} className="text-sm font-medium text-teal-700 hover:underline">Edit</button>
      <button onClick={onDelete} className="text-sm font-medium text-rose-700 hover:underline">Delete</button>
    </div>
  </li>
}

function EventsPage() {
  const queryClient = useQueryClient()
  const [typeFilter, setTypeFilter] = useState<LifeEventType | ''>('')
  const [eventDate, setEventDate] = useState('')
  const [editingEvent, setEditingEvent] = useState<LifeEvent | null>(null)
  const [showForm, setShowForm] = useState(false)

  const eventsQuery = useQuery({ queryKey: ['events', typeFilter], queryFn: () => eventsApi.getEvents(typeFilter || undefined) })
  const createMutation = useMutation({ mutationFn: eventsApi.createEvent, onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['events'] }); setShowForm(false) } })
  const updateMutation = useMutation({ mutationFn: ({ id, request }: { id: number, request: LifeEventRequest }) => eventsApi.updateEvent(id, request), onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['events'] }); setEditingEvent(null) } })
  const deleteMutation = useMutation({ mutationFn: eventsApi.deleteEvent, onSuccess: () => queryClient.invalidateQueries({ queryKey: ['events'] }) })
  const filteredEvents = (eventsQuery.data ?? []).filter((event) => !eventDate || eventDateKey(event.eventTime) === eventDate)

  return <Layout><section className="py-10">
    <p className="text-sm font-semibold tracking-[0.14em] text-teal-700">LIFE EVENTS</p>
    <h1 className="mt-3 text-4xl font-semibold sm:text-5xl">Events</h1>
    <div className="mt-6 flex flex-wrap items-center gap-4">
      <select value={typeFilter} onChange={(event) => setTypeFilter(event.target.value as LifeEventType | '')} className="border border-stone-300 p-2">
        <option value="">All types</option>
        {LIFE_EVENT_TYPES.map((type) => <option key={type} value={type}>{type}</option>)}
      </select>
      <label className="flex items-center gap-2 text-sm text-stone-600">Activity date <input type="date" value={eventDate} onChange={(event) => setEventDate(event.target.value)} className="border border-stone-300 bg-white p-2 text-stone-900" /></label>
      {(typeFilter || eventDate) && <button onClick={() => { setTypeFilter(''); setEventDate('') }} className="text-sm font-medium text-teal-700 underline">Clear filters</button>}
      <button onClick={() => { setShowForm((current) => !current); setEditingEvent(null) }} className="border border-stone-400 px-4 py-2 font-medium hover:border-teal-700">{showForm ? 'Cancel' : 'Add event'}</button>
    </div>
    <p className="mt-3 text-sm text-stone-500">Date filters use known activity times; events with unknown times remain unfiltered only when no date is selected.</p>
    {showForm && <EventForm submitLabel="Save event" isPending={createMutation.isPending} error={createMutation.error?.message} onSubmit={(request) => createMutation.mutate(request)} />}
    {editingEvent && <EventForm initialEvent={editingEvent} submitLabel="Save changes" isPending={updateMutation.isPending} error={updateMutation.error?.message} onSubmit={(request) => updateMutation.mutate({ id: editingEvent.id, request })} />}
    {eventsQuery.isPending && <p className="mt-10 text-stone-600">Loading events...</p>}
    {eventsQuery.isError && <p role="alert" className="mt-10 text-rose-700">{eventsQuery.error.message}</p>}
    {eventsQuery.data?.length === 0 && <p className="mt-10 text-stone-600">No events recorded yet.</p>}
    {eventsQuery.data && eventsQuery.data.length > 0 && filteredEvents.length === 0 && <p className="mt-10 text-stone-600">No events match these filters. Unknown activity times are not included in date matches.</p>}
    <ul className="mt-8">
      {filteredEvents.map((event) => <EventListItem key={event.id} event={event} onEdit={() => { setEditingEvent(event); setShowForm(false) }} onDelete={() => deleteMutation.mutate(event.id)} />)}
    </ul>
    {deleteMutation.isError && <p role="alert" className="mt-4 text-rose-700">{deleteMutation.error.message}</p>}
  </section></Layout>
}

function JournalEventsSection({ journalId, onExtract, isExtracting, extractionError }: { journalId: number, onExtract: () => void, isExtracting: boolean, extractionError?: string }) {
  const eventsQuery = useQuery({ queryKey: ['journals', journalId, 'events'], queryFn: () => eventsApi.getJournalEvents(journalId), enabled: Number.isFinite(journalId) })

  return <section className="mt-10 border-t border-stone-300 pt-8">
    <div className="flex flex-wrap items-center justify-between gap-4"><p className="text-sm font-semibold tracking-[0.14em] text-teal-700">LIFE EVENTS FROM THIS ENTRY</p><button onClick={onExtract} disabled={isExtracting} className="bg-teal-700 px-4 py-2 font-semibold text-white transition hover:bg-teal-800 disabled:cursor-wait disabled:bg-stone-400">{isExtracting ? 'Extracting...' : 'Extract activities'}</button></div>
    <p className="mt-2 text-sm text-stone-500">Extracted events are linked to this journal. Re-extraction replaces only its previous JOURNAL events.</p>
    {extractionError && <p role="alert" className="mt-4 text-rose-700">{extractionError}</p>}
    {eventsQuery.isPending && <p className="mt-4 text-stone-600">Loading events...</p>}
    {eventsQuery.isError && <p role="alert" className="mt-4 text-rose-700">{eventsQuery.error.message}</p>}
    {eventsQuery.data?.length === 0 && <p className="mt-4 text-stone-600">No events linked to this entry yet.</p>}
    <ul className="mt-4">
      {eventsQuery.data?.map((event) => <li key={event.id} className="border-b border-stone-300 py-3"><div className="flex items-center justify-between gap-3"><p className="font-semibold text-teal-700">{event.type}</p><p className="text-xs uppercase tracking-wide text-stone-400">Source: {event.source}</p></div><p className="mt-1 text-stone-700">{eventSummary(event)}</p></li>)}
    </ul>
    <Link to="/events" className="mt-4 inline-block text-teal-700 underline">Manage all events</Link>
  </section>
}

function App() {
  return <Routes><Route path="/" element={<ComposePage />} /><Route path="/dashboard" element={<DashboardPage />} /><Route path="/journals/new" element={<ComposePage />} /><Route path="/journals" element={<JournalHistoryPage />} /><Route path="/journals/:id" element={<JournalDetailPage />} /><Route path="/events" element={<EventsPage />} /><Route path="*" element={<Navigate to="/dashboard" replace />} /></Routes>
}

export default App
