import {combineReducers, configureStore} from '@reduxjs/toolkit';
import {FLUSH, PAUSE, PERSIST, persistReducer, PURGE, REGISTER, REHYDRATE,} from 'redux-persist';
import storage from 'redux-persist/lib/storage'; // defaults to localStorage for web
import uiReducer from '@/services/state/features/ui/uiSlice';
import searchReducer from '@/services/state/features/search/searchSlice';
import notificationReducer from '@/services/state/features/notification/notificationSlice';
import { PreloadedState } from '@reduxjs/toolkit'; // Added this import

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

export const makeStore = (initialState?: PreloadedState<RootState>) => {
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

// Infer the `RootState` and `AppStore` types from the store itself
export type RootState = ReturnType<typeof rootReducer>;
export type AppStore = ReturnType<typeof makeStore>;
export type AppDispatch = AppStore['dispatch'];
