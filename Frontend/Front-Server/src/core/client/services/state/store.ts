import { combineReducers, configureStore } from '@reduxjs/toolkit';
import {
    persistReducer,
    FLUSH,
    REHYDRATE,
    PAUSE,
    PERSIST,
    PURGE,
    REGISTER,
} from 'redux-persist';
import storage from 'redux-persist/lib/storage';
import sessionStorage from 'redux-persist/lib/storage/session'; // Import sessionStorage

import uiReducer from '@/core/client/services/state/features/ui/uiSlice';
import searchReducer from '@/core/client/services/state/features/search/searchSlice';
import notificationReducer from '@/core/client/services/state/features/notification/notificationSlice';
import themeReducer from '@/core/client/services/state/features/theme/themeSlice';

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
/* persisted reducer (for search and theme, using localStorage) */
/* ------------------------------------------------------------------ */
const persistedReducer = persistReducer(
    {
        key: 'root',
        storage, // localStorage
        whitelist: ['search', 'theme'], // Only search and theme for localStorage
    },
    rootReducer
);

/* ------------------------------------------------------------------ */
/* store factory */
/* ------------------------------------------------------------------ */
export const makeStore = (initialState?: Partial<RootState>) =>
    configureStore({
        reducer: persistedReducer, // This will wrap the rootReducer, which already includes uiPersistedReducer
        preloadedState: initialState as RootState,
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

