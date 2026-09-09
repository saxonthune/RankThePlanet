---
title: Writing register, jargon, and nominalization
summary: Empirical findings on how jargon and nominalization affect reader comprehension and persuasion; the zombie-noun frame as a mechanical revision rule for agent-authored docs
tags: [research, writing, language, jargon, nominalization, style]
deps: [doc00.03]
---

# Writing register, jargon, and nominalization

The question this session investigates: would forbidding HN/Reddit/SV-engineering register in agent-authored rhidoc docs, code comments, and PR descriptions — "no-op" used as a verb, "the ask", "a deliverable", "backlog" as a noun verb, "an unblock" — measurably improve outcomes for human readers and for downstream code? Sub-question: is the impulse to suppress that register a real linguistic critique, or aesthetic preference dressed in a frame?

The answer the literature supports: the impulse points at something real, but the target is **nominalization and agent-suppression**, not a vocabulary list. The mechanism is grammatical, not tribal.

## 1. Jargon disrupts processing fluency — and inline definitions do not rescue it

The strongest evidence on register comes from three randomized experiments in science communication.

Bullock et al. 2019 ran a 2×2 factorial (N=650, lay readers, randomized) on jargon and on inline definitions. The jargon condition reduced self-reported processing fluency from M=5.27 to M=4.57 on a 7-point scale, F(1,636)=76.03, p<.001, η²=.11. The definitions condition had no main effect (p=.543) and no interaction with jargon. The harm operates via fluency rather than comprehension proper, and definitions do not lift the cost.

Shulman et al. 2020 replicated and extended the result: jargon disrupts readers' ability to fluently process scientific information, with definitions present or absent. The fluency hit then propagates: jargon reduces social identification with the speaker's community, which reduces self-reported interest and perceived understanding.

Bullock et al. 2020 tested across three topics with varying urgency (COVID-19, flood policy, regulatory policy). For non-urgent flood content jargon harmed persuasion via fluency (B=-0.47, p<.05); for non-urgent policy content the effect was stronger (B=-0.74, p<.001); for high-urgency COVID-19 the jargon effect vanished (B=0.11, p=.598). When the reader has a reason to push through, the fluency cost neutralizes.

The implication for agent-authored rhidoc docs: the cost is real for the reader who is browsing — picking up a doc to orient — and small for the reader who is debugging a known problem and needs the information at any register.

## 2. The mechanism: nominalization and zombie-noun subjects

The Williams / Sword / Pinker tradition supplies the grammatical frame that fits the user's intuition.

A **nominalization** is a verb or adjective turned into a noun via suffixes like *-tion*, *-ment*, *-ity*, *-ance*, *-ism*. The defect is not the suffix itself — *evaluation* and *implementation* are sometimes the right word — but the structural pattern that nominalization enables: writers drop the human subject and let the abstraction sit on the page without an actor.

DeScioli & Pinker 2021 (PS: Political Science & Politics) audited a single issue of the *American Political Science Review* (vol 113 iss 2) and documented 1,000+ instances of what they call **heavy noun phrases** — driven by five contributors: piled modifiers, needless words, nebulous nouns, missing prepositions, and buried verbs. Their Table A4 supplies a mechanical revision recipe with examples like *"incumbent malfeasance revelations"* → *"revealing an incumbent's misspending"*, and *"regulatory design process"* → *"designing regulations"*.

Sword's diagnostic, quoted in LSU's writing guide: *"every sentence has a zombie noun or a pronoun as its subject, coupled with an uninspiring verb."* Pinker's reformulation in *The Sense of Style*: when a sentence has no concrete actor, the writer has hidden who is doing what.

The HN/SV register the user finds objectionable maps onto this frame precisely. *"The ask"* nominalizes *ask*; *"a deliverable"* nominalizes *deliver*; *"an unblock"* nominalizes *unblock*; *"a backlog"* used as a verb-source is the same shape inverted (a noun used where the verb was). *"No-op"* as a verb is the inverse pathology — a noun pressed into verbal duty. All four examples suppress the agent: the sentence is *about* something happening rather than someone doing something.

This is a real, named, grammatically diagnosable defect. It is not aesthetic preference.

## 3. The "jargon signals lower-quality thinking" claim does not survive

Sword has been quoted as arguing that heavy nominalization transmits a social signal — that big-word users come across as smarter, and that students absorb this. The adversarial verification pass on the deep-research report killed this claim (1-2 refute vote). The evidence does not support the *signal* reading; it supports the *mechanism* reading. The reader pays a fluency cost regardless of what the register signals about the writer.

Translated for this workspace: do not claim that the HN register *indicates* lower-quality thinking. Claim that the constructions it favors *carry* documented processing costs, agent-suppression among them.

## 4. LLM output: the evidence is thin and mixed

The user's secondary hypothesis — that forcing the agent away from HN register would improve generated code — is **not supported by current literature, and is not refuted either**. No published study directly tests jargon density or nominalization rate in prompts against code-quality metrics.

The closest study, Della Porta, Lambiase, and Palomba (EASE 2025), evaluated 7,583 ChatGPT-generated code files across prompt *structural* patterns (zero-shot, chain-of-thought, few-shot, persona) using Kruskal-Wallis tests on maintainability, security, and reliability. The authors conclude prompt structure does not significantly affect these quality metrics. The study isolates structure, not register.

Politeness effects are non-monotonic. Yin et al. 2024 (English, Chinese, Japanese; multiple LLMs) report that *"impolite prompts often result in poor performance, but overly polite language does not guarantee better outcomes"* — the sweet spot varies by language and model. Dobariya & Kumar 2025 (one preprint, ChatGPT-4o, 250 prompts) found rude prompts slightly outperformed polite ones (84.8% vs 80.8%). The result is one preprint and should not be over-generalized.

Computational readability formulas — Flesch-Kincaid, Dale-Chall, Gunning Fog, and frontier-LLM-based scoring — are poor predictors of actual human reading ease (Gruteke Klein et al. 2025, eye-tracking validation). Simpler features (surprisal, word length, word frequency) outperform comprehensive formulas. Operationalizing "plain language" via a Flesch score is a category error.

The honest summary for agent-output quality: the intuition is plausible, no one has measured it, and proxies like Flesch scores will not catch the relevant signal.

## 5. What moves the needle versus what is cosmetic

Cosmetic substitutions do not target the documented mechanism. Replacing *utilize* with *use*, dropping *"hey"* from a message, or banning a vocabulary list of HN slang while leaving the nominalizations around them in place — these are surface edits. The reader's fluency cost is driven by the heavy-noun-phrase pattern, not the individual words.

The revision rules the literature supports, ranked by signal-to-cosmetic ratio:

1. **Name a concrete actor as the grammatical subject.** Replace agent-less subjects (*"the ask"*, *"an unblock"*) with the person or system doing the work.
2. **Convert nominalizations back to verbs.** *"perform a verification of X"* → *"verify X"*; *"there is a need for the addition of"* → *"add"*.
3. **Eliminate zombie-noun subjects coupled with weak verbs** — *is, has, makes, performs, conducts, undertakes*. These pair invariably with a buried verb. Find the verb, promote it, drop the scaffolding.
4. **Prefer agentive finite verbs over *-tion*, *-ment*, *-ity*, *-ance* abstractions** — unless the abstraction names a real thing (*evaluation* as a noun for the artifact produced, not as a verb-replacement).
5. **Measure mechanically rather than by feel.** The NIHR plain-language summary corpus shows only 21.7% (269/1,241) of summaries — written by experts explicitly for lay audiences — met De-Jargonizer jargon thresholds. Writers systematically underestimate their own register.

The vocabulary list the user objects to falls out of these rules as a side effect, not as the rule itself. *"The ask is to do X"* fails rule 1 and rule 2; rewritten as *"do X"* it passes both, and the offending noun-phrase disappears.

## 6. Implication for this workspace

The artifact downstream is a CLAUDE.md rule — see the *"Agentive prose, not zombie nouns"* section there — that targets the mechanism rather than the vocabulary. The rule is two sentences: name a concrete actor as the subject, and let verbs be verbs. The rest follows.

The rhidoc convention document (doc00.03) gains a cross-reference: writing-style guidance specifies that doc prose should follow the same rule. Sidecar JSONs and code comments inherit the rule; descriptive fields in `*.statechart.json` and inventory affordances should describe a surface *doing* something, not *the doing of* something.

## 7. Caveats

The human-reader evidence sits entirely in science communication contexts — lay readers consuming text about GMOs, self-driving cars, COVID-19, flood policy, and regulatory design. The urgency-moderation finding suggests that engineers reading their own team's PR description occupy the high-motivation regime where the jargon penalty shrinks. Generalization from lay-reader contexts to expert-to-expert engineering communication is not directly tested.

For technical jargon that carries a precise referent — *idempotent*, *monomorphization*, *no-op* as a name for a transition with no effect — the Bullock fluency cost may be partly rescued by expertise the way urgency rescued it in the COVID condition. The literature does not test this directly.

The LLM-register evidence is preliminary and model-specific. Findings from 2024–2025 on ChatGPT-4o and similar should not be projected forward several model generations.

## 8. Open questions

- Does the jargon-fluency penalty replicate in expert-to-expert technical writing (engineering specs, PR descriptions read by teammates)?
- Does prompt register, controlled independently of structural pattern and politeness, measurably affect LLM code or reasoning quality?
- For technical jargon with precise referents, does engineer-level expertise rescue fluency the way reader urgency does?
- Does applying the *"let verbs be verbs"* rule to LLM system prompts produce measurable downstream differences in output structure, agent-naming, or specification clarity?

## 9. Sources

Primary — randomized experiments and corpus studies:

- Bullock, Colón Amill, Shulman & Dixon (2019). [Jargon as a barrier to effective science communication](https://comm.osu.edu/sites/comm.osu.edu/files/PUS%202019-%20Bullock%20et%20al..pdf). *Public Understanding of Science*. N=650 randomized; fluency results.
- Shulman, Dixon, Bullock & Colón Amill (2020). [The effects of jargon on processing fluency, self-perceptions, and scientific engagement](https://journals.sagepub.com/doi/10.1177/0261927X20902177). *Journal of Language and Social Psychology*. Social-identification mediation.
- Bullock, Shulman & Huskey (2020). [Jargon and the limits of public communication of science](https://pmc.ncbi.nlm.nih.gov/articles/PMC7540871/). COVID-19 / flood / policy urgency moderation.
- DeScioli & Pinker (2021). [Piled modifiers and other heavy noun phrases in academic writing](https://stevenpinker.com/files/pinker/files/descioli_pinker_2021_piled_modifiers.pdf). *PS: Political Science & Politics*. Single-issue audit; mechanical revision table.
- Marchant et al. (2024). [Jargon in NIHR plain-language summaries](https://pmc.ncbi.nlm.nih.gov/articles/PMC11773280/). 1,241-summary corpus; only 21.7% pass De-Jargonizer thresholds.
- Della Porta, Lambiase & Palomba (2025). [Prompt patterns and code quality](https://arxiv.org/pdf/2504.13656). EASE 2025; 7,583 ChatGPT files; Kruskal-Wallis on maintainability/security/reliability.
- Yin et al. (2024). [Should we respect LLMs? A cross-lingual study on the influence of prompt politeness on LLM performance](https://arxiv.org/abs/2402.14531).
- Dobariya & Kumar (2025). [On the politeness of LLMs](https://arxiv.org/abs/2510.04950). ChatGPT-4o rude-vs-polite preprint.
- Gruteke Klein et al. (2025). [The reliability of automated readability metrics](https://arxiv.org/pdf/2502.11150). Eye-tracking validation; formulas and frontier LLMs both poor predictors.

Secondary — writing-style frame:

- LSU University Writing Program. [Nominalization and zombie nouns](https://www.lsu.edu/hss/english/files/university_writing_files/item51054.pdf). Sword diagnostic for student writers.
