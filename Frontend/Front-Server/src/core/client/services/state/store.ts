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
import type { PersistState, PersistConfig } from 'redux-persist/es/types'; // Correctly import PersistState
import storage from 'redux-persist/lib/storage';
import sessionStorage from 'redux-persist/lib/storage/session'; // Import sessionStorage

import uiReducer from '@/core/client/services/state/features/ui/uiSlice';
import searchReducer from '@/core/client/services/state/features/search/searchSlice';
import notificationReducer from '@/core/client/services/state/features/notification/notificationSlice';
import themeReducer from '@/core/client/services/state/features/theme/themeSlice';
import { createTransform } from 'redux-persist';

/* ------------------------------------------------------------------ */
/* ui reducer with sessionStorage (per session) */
/* ------------------------------------------------------------------ */
const uiPersistConfig = {
    key: 'ui',
    storage: sessionStorage,
    whitelist: ['isSidebarOpen', 'userMode', 'viewResetCounter', 'CANDIDATE', 'RECRUITER'], // Whitelist properties of ui slice
};

const uiPersistedReducer = persistReducer(uiPersistConfig, uiReducer);

/* ------------------------------------------------------------------ */
/* root reducer (NON-persisted) */
/* ------------------------------------------------------------------ */
export const rootReducer = combineReducers({
    ui: uiPersistedReducer, // Use the persisted ui reducer
    search: searchReducer,
    notification: notificationReducer,
    theme: themeReducer,
});

export type RootState = ReturnType<typeof rootReducer>;

/* ------------------------------------------------------------------ */
/* Transform to exclude matches from search persistence */
/* topSkills and totalCount are persisted for SearchResultAnalysisPanel */
/* ------------------------------------------------------------------ */
const searchTransform = createTransform(
    // Transform state on the way to storage
    (inboundState: any) => {
        // For each mode (CANDIDATE, RECRUITER), remove only matches
        const transformedState = { ...inboundState };
        ['CANDIDATE', 'RECRUITER'].forEach((mode) => {
            if (transformedState[mode]) {
                transformedState[mode] = {
                    ...transformedState[mode],
                    matches: [], // Don't persist matches (large data, can be stale)
                    // topSkills and totalCount are persisted
                };
            }
        });
        return transformedState;
    },
    // Transform state when rehydrating (on the way out of storage)
    (outboundState: any) => outboundState,
    { whitelist: ['search'] } // Only apply to search reducer
);

/* ------------------------------------------------------------------ */
/* persisted reducer (for search and theme, using localStorage) */
/* ------------------------------------------------------------------ */
const rootPersistConfig: PersistConfig<RootState> = {
    key: 'root',
    storage, // localStorage
    whitelist: ['search', 'theme'], // Only search and theme for localStorage
    transforms: [searchTransform], // Apply transform to exclude matches
};

const finalPersistedReducer = persistReducer(
    rootPersistConfig,
    rootReducer as any // Type assertion needed due to nested persist
);

// Define the type for the store's state, which now includes the root-level _persist
export type PersistedStoreState = RootState & { _persist: PersistState }; // Use PersistState here

/* ------------------------------------------------------------------ */
/* store factory */
/* ------------------------------------------------------------------ */
export const makeStore = (initialState?: Partial<PersistedStoreState>) =>
    configureStore({
        reducer: finalPersistedReducer, // This will wrap the rootReducer, which already includes uiPersistedReducer
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

