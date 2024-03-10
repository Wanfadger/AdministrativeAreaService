package com.wanfadger.AdministrativeareaApi.shared.security;

//@Component
//extends OncePerRequestFilter
//@RequiredArgsConstructor
public class JwtAuthenticationFilter  {
//    private final JwtService jwtService;
//    private final UserDetailsService userDetailsService;
//    private final ObjectMapper objectMapper;
//
//    @SneakyThrows
//    @Override
//    protected void doFilterInternal(@NonNull HttpServletRequest request,
//                                    @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws IOException {
//
//        try {
//            final String authHeader = request.getHeader("Authorization");
//            final String jwt;
//            final String userEmail;
//            if (authHeader == null || authHeader.isEmpty()|| !authHeader.startsWith("Bearer ")) {
//                filterChain.doFilter(request, response);
//                return;
//            }
//
//
//            jwt = authHeader.substring(7);
//            userEmail = jwtService.extractUsername(jwt);
//
//            if (userEmail != null && !userEmail.isEmpty()
//                    && SecurityContextHolder.getContext().getAuthentication() == null) {
//
//
//                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
//                if (jwtService.isTokenValid(jwt, userDetails)) {
//                    SecurityContext context = SecurityContextHolder.createEmptyContext();
//                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
//                            userDetails, null, userDetails.getAuthorities());
//                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                    context.setAuthentication(authToken);
//                    SecurityContextHolder.setContext(context);
//                }
//            }
//            filterChain.doFilter(request, response);
//        }
//        catch (SignatureException e) {
//            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid token");
//            problemDetail.setInstance(new URI(request.getRequestURI()));
//            // custom error response class used across my project
//            response.setStatus(HttpStatus.BAD_REQUEST.value());
//            response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
//            response.setContentType("application/json");
//
//        }
//        catch (ExpiredJwtException e) {
//            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
//            problemDetail.setInstance(new URI(request.getRequestURI()));
//            // custom error response class used across my project
//            response.setStatus(HttpStatus.UNAUTHORIZED.value());
//            response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
//            response.setContentType("application/json");
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
//            problemDetail.setInstance(new URI(request.getRequestURI()));
//            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
//            response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
//            response.setContentType("application/json");
//        }

//    }


}