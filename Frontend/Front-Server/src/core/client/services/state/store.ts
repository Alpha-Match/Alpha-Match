import { combineReducers, configureStore, AnyAction } from '@reduxjs/toolkit';
import {
    persistReducer,
    FLUSH,
    REHYDRATE,
    PAUSE,
    PERSIST,
    PURGE,
    REGISTER,
} from 'redux-persist';
import type { PersistState, PersistConfig } from 'redux-persist/es/types';
import storage from 'redux-persist/lib/storage';
import sessionStorage from 'redux-persist/lib/storage/session';

import uiReducer from '@/core/client/services/state/features/ui/uiSlice';
import searchReducer, { initialModeSpecificState } from '@/core/client/services/state/features/search/searchSlice';
import notificationReducer from '@/core/client/services/state/features/notification/notificationSlice';
import themeReducer from '@/core/client/services/state/features/theme/themeSlice';
import { createTransform } from 'redux-persist';
import { UserMode } from '@/types';

/* ------------------------------------------------------------------ */
/* ui reducer with sessionStorage (per session) */
/* ------------------------------------------------------------------ */
const uiPersistConfig = {
    key: 'ui',
    storage: sessionStorage,
    whitelist: ['isSidebarOpen', 'userMode', 'viewResetCounter', 'CANDIDATE', 'RECRUITER'],
};

const uiPersistedReducer = persistReducer(uiPersistConfig, uiReducer);

/* ------------------------------------------------------------------ */
/* root reducer (NON-persisted) */
/* ------------------------------------------------------------------ */
export const rootReducer = combineReducers({
    ui: uiPersistedReducer,
    search: searchReducer,
    notification: notificationReducer,
    theme: themeReducer,
});

export type RootState = ReturnType<typeof rootReducer>;

/* ------------------------------------------------------------------ */
/* Transform to handle search persistence */
/* ------------------------------------------------------------------ */
const searchTransform = createTransform(
    // Inbound: Transform state on the way to storage
    (inboundState: any, key) => {
        const transformedState = { ...inboundState };
        // Persist everything except 'matches' for each mode
        [UserMode.CANDIDATE, UserMode.RECRUITER].forEach((mode) => {
            if (transformedState[mode]) {
                const { matches, ...rest } = transformedState[mode];
                transformedState[mode] = rest;
            }
        });
        return transformedState;
    },
    // Outbound: Transform state when rehydrating
    (outboundState: any, key) => {
        if (!outboundState) return outboundState;

        const rehydratedState = { ...outboundState };
        [UserMode.CANDIDATE, UserMode.RECRUITER].forEach((mode) => {
            // Merge rehydrated state with initial state to ensure all fields exist
            rehydratedState[mode] = {
                ...initialModeSpecificState,
                ...(rehydratedState[mode] || {}),
                 matches: [], // Always reset matches on rehydration
            };
        });
        return rehydratedState;
    },
    { whitelist: ['search'] }
);

/* ------------------------------------------------------------------ */
/* persisted reducer (for search and theme, using localStorage) */
/* ------------------------------------------------------------------ */
const rootPersistConfig: PersistConfig<RootState> = {
    key: 'root',
    storage, // localStorage
    whitelist: ['search', 'theme'],
    transforms: [searchTransform],
};

const finalPersistedReducer = persistReducer(
    rootPersistConfig,
    rootReducer as any
);

export type PersistedStoreState = RootState & { _persist: PersistState };

/* ------------------------------------------------------------------ */
/* store factory */
/* ------------------------------------------------------------------ */
export const makeStore = (initialState?: Partial<PersistedStoreState>) =>
    configureStore({
        reducer: finalPersistedReducer,
        preloadedState: initialState as PersistedStoreState,
        middleware: (getDefaultMiddleware) =>
            getDefaultMiddleware({
                serializableCheck: {
                    ignoredActions: [
                        FLUSH,
                        REHYDRATE,
                        PAUSE,
                        PERSIST,
                        PURGE,
                        REGISTER,
                    ],
                },
            }),
    });

export type AppStore = ReturnType<typeof makeStore>;
export type AppDispatch = AppStore['dispatch'];

