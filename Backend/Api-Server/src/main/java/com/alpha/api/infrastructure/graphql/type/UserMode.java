package com.alpha.api.infrastructure.graphql.type;

/**
 * UserMode (GraphQL Enum)
 * - Frontend compatible enum
 * - CANDIDATE: Job seeker searching for jobs (searches Recruit table)
 * - RECRUITER: Recruiter searching for candidates (searches Candidate table)
 */
public enum UserMode {
    CANDIDATE,
    RECRUITER
}
