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

import uiReducer from '@/services/state/features/ui/uiSlice';
import searchReducer from '@/services/state/features/search/searchSlice';
import notificationReducer from '@/services/state/features/notification/notificationSlice';

/* ------------------------------------------------------------------ */
/* root reducer (NON-persisted) */
/* ------------------------------------------------------------------ */
export const rootReducer = combineReducers({
    ui: uiReducer,
    search: searchReducer,
    notification: notificationReducer,
});

export type RootState = ReturnType<typeof rootReducer>;

/* ------------------------------------------------------------------ */
/* persisted reducer */
/* ------------------------------------------------------------------ */
const persistedReducer = persistReducer(
    {
        key: 'root',
        storage,
        whitelist: ['ui', 'search'],
    },
    rootReducer
);

/* ------------------------------------------------------------------ */
/* store factory */
/* ------------------------------------------------------------------ */
export const makeStore = (initialState?: Partial<RootState>) =>
    configureStore({
        reducer: persistedReducer,
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
