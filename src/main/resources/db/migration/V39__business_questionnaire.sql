CREATE TABLE public.business_questionnaires (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES public.clients (id) ON DELETE CASCADE,
    ad_request_id UUID UNIQUE REFERENCES public.ad_requests (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (now())
);

CREATE UNIQUE INDEX uq_business_questionnaire_client_draft
    ON public.business_questionnaires (client_id)
    WHERE ad_request_id IS NULL;

CREATE INDEX idx_business_questionnaires_client_id ON public.business_questionnaires (client_id);

CREATE TABLE public.business_questionnaire_revisions (
    id UUID PRIMARY KEY,
    questionnaire_id UUID NOT NULL REFERENCES public.business_questionnaires (id) ON DELETE CASCADE,
    version INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (now()),
    created_by_client_id UUID REFERENCES public.clients (id) ON DELETE SET NULL,
    CONSTRAINT uq_bqr_questionnaire_version UNIQUE (questionnaire_id, version)
);

CREATE INDEX idx_business_questionnaire_revisions_qid ON public.business_questionnaire_revisions (questionnaire_id);

CREATE TABLE public.business_questionnaire_answers (
    id UUID PRIMARY KEY,
    revision_id UUID NOT NULL REFERENCES public.business_questionnaire_revisions (id) ON DELETE CASCADE,
    question_key VARCHAR(80) NOT NULL,
    answer_text TEXT NOT NULL,
    CONSTRAINT uq_bqa_revision_key UNIQUE (revision_id, question_key)
);

CREATE INDEX idx_business_questionnaire_answers_revision ON public.business_questionnaire_answers (revision_id);

INSERT INTO public.business_questionnaires (id, client_id, ad_request_id, created_at, updated_at)
SELECT gen_random_uuid(), ar.client_id, ar.id, now(), now()
FROM public.ad_requests ar
WHERE NOT EXISTS (
    SELECT 1 FROM public.business_questionnaires bq WHERE bq.ad_request_id = ar.id
);

INSERT INTO public.business_questionnaire_revisions (id, questionnaire_id, version, created_at, created_by_client_id)
SELECT gen_random_uuid(), bq.id, 1, COALESCE(ar.created_at, now()), ar.client_id
FROM public.business_questionnaires bq
JOIN public.ad_requests ar ON ar.id = bq.ad_request_id
WHERE NOT EXISTS (
    SELECT 1 FROM public.business_questionnaire_revisions r
    WHERE r.questionnaire_id = bq.id
);

INSERT INTO public.business_questionnaire_answers (id, revision_id, question_key, answer_text)
SELECT gen_random_uuid(), r.id, 'LEGACY_SLOGAN', TRIM(ar.slogan)
FROM public.ad_requests ar
JOIN public.business_questionnaires bq ON bq.ad_request_id = ar.id
JOIN public.business_questionnaire_revisions r ON r.questionnaire_id = bq.id AND r.version = 1
WHERE ar.slogan IS NOT NULL AND TRIM(ar.slogan) <> '';

INSERT INTO public.business_questionnaire_answers (id, revision_id, question_key, answer_text)
SELECT gen_random_uuid(), r.id, 'LEGACY_BRAND_GUIDELINE_URL', TRIM(ar.brand_guideline_url)
FROM public.ad_requests ar
JOIN public.business_questionnaires bq ON bq.ad_request_id = ar.id
JOIN public.business_questionnaire_revisions r ON r.questionnaire_id = bq.id AND r.version = 1
WHERE ar.brand_guideline_url IS NOT NULL AND TRIM(ar.brand_guideline_url) <> '';
