import { QueryClient } from "@tanstack/react-query";

/**
 * Single application-wide QueryClient. Exported so the socket bridge can call
 * setQueryData directly with the exact keys used by the data hooks.
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 3_000,
      retry: 1,
    },
  },
});