import { configureStore, combineReducers } from '@reduxjs/toolkit';
import {
  persistStore,
  persistReducer,
  FLUSH,
  REHYDRATE,
  PAUSE,
  PERSIST,
  PURGE,
  REGISTER,
} from 'redux-persist';
import storage from 'redux-persist/lib/storage'; // defaults to localStorage for web

import uiReducer from './features/ui/uiSlice';
import searchReducer from './features/search/searchSlice';
import notificationReducer from './features/notification/notificationSlice';

export const persistConfig = {
  key: 'root',
  storage,
  whitelist: ['ui', 'search'], // only persist these reducers
};

export const rootReducer = combineReducers({
  ui: uiReducer,
  search: searchReducer,
  notification: notificationReducer,
});

export const makeStore = (initialState?: RootState) => {
  const store = configureStore({
    reducer: persistReducer(persistConfig, rootReducer), // Apply persistReducer here
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware({
        serializableCheck: {
          ignoredActions: [FLUSH, REHYDRATE, PAUSE, PERSIST, PURGE, REGISTER],
        },
      }),
    preloadedState: initialState, // Add preloadedState for SSR if needed
  });
  return store;
};

// Types remain the same
export type AppStore = ReturnType<typeof makeStore>;
export type RootState = ReturnType<AppStore['getState']>;
export type AppDispatch = AppStore['dispatch'];
