<?php
/**
 * Plugin Name: InnerNet WebApp
 * Description: Stellt die InnerNet Webapp als Vollbild ohne WordPress-Menü oder Footer bereit und bringt alle benötigten Assets mit.
 * Version: 0.1.17
 * Author: InnerNet
 */

if (!defined('ABSPATH')) {
    exit;
}

const INNERNET_WEBAPP_QUERY_KEY = 'innernet_app';
const INNERNET_WEBAPP_ROUTE_SLUG = 'innernet-app';
const INNERNET_WEBAPP_PAGE_SLUG = 'innernet-webapp';
const INNERNET_WEBAPP_PAGE_OPTION = 'innernet_webapp_page_id';

/**
 * Register query var for the dedicated app route.
 */
function innernet_webapp_register_query_var(array $vars): array
{
    $vars[] = INNERNET_WEBAPP_QUERY_KEY;
    return $vars;
}
add_filter('query_vars', 'innernet_webapp_register_query_var');

/**
 * Create a pretty permalink for the app route.
 */
function innernet_webapp_add_rewrite_rule(): void
{
    add_rewrite_rule(
        '^' . INNERNET_WEBAPP_ROUTE_SLUG . '/?$',
        'index.php?' . INNERNET_WEBAPP_QUERY_KEY . '=1',
        'top'
    );
}
add_action('init', 'innernet_webapp_add_rewrite_rule');

register_activation_hook(__FILE__, function (): void {
    innernet_webapp_ensure_page();
    innernet_webapp_add_rewrite_rule();
    flush_rewrite_rules();
});

register_deactivation_hook(__FILE__, function (): void {
    flush_rewrite_rules();
});

/**
 * Create or retrieve the dedicated page used to host the app without theme chrome.
 */
function innernet_webapp_ensure_page(): ?int
{
    $page_id = (int) get_option(INNERNET_WEBAPP_PAGE_OPTION, 0);
    $page = $page_id ? get_post($page_id) : null;

    if ($page && $page instanceof WP_Post) {
        return $page_id;
    }

    // Try to find an existing page with the same slug to avoid duplicates.
    $existing = get_page_by_path(INNERNET_WEBAPP_PAGE_SLUG);
    if ($existing && $existing instanceof WP_Post) {
        update_option(INNERNET_WEBAPP_PAGE_OPTION, $existing->ID);
        return (int) $existing->ID;
    }

    $page_id = wp_insert_post([
        'post_title'   => 'InnerNet Webapp',
        'post_name'    => INNERNET_WEBAPP_PAGE_SLUG,
        'post_status'  => 'publish',
        'post_type'    => 'page',
        'post_content' => 'InnerNet Webapp',
    ]);

    if (!is_wp_error($page_id)) {
        update_option(INNERNET_WEBAPP_PAGE_OPTION, $page_id);
        return (int) $page_id;
    }

    return null;
}

/**
 * Render the SPA in a blank, full-screen document without the theme.
 */
function innernet_webapp_template_redirect(): void
{
    $page_id = (int) get_option(INNERNET_WEBAPP_PAGE_OPTION, 0);
    $is_app_page = $page_id && is_page($page_id);
    $is_route = (bool) get_query_var(INNERNET_WEBAPP_QUERY_KEY);

    if (!$is_route && !$is_app_page) {
        return;
    }

    status_header(200);
    nocache_headers();
    show_admin_bar(false);

    $app_base_raw = trailingslashit(plugins_url('app', __FILE__));
    $content_base_raw = trailingslashit(plugins_url('content', __FILE__));
    $app_base_attr = esc_url($app_base_raw);
    $content_base_attr = esc_url($content_base_raw);
    $app_base_js = esc_js($app_base_raw);
    $content_base_js = esc_js($content_base_raw);

    // All asset references stay relative to the plugin directory.
    $html = <<<HTML
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>InnerNet</title>
    <base href="{$app_base_attr}">
    <link rel="stylesheet" href="styles.css">
    <style>
        html, body { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; background: #0b0c0f; }
        #root { width: 100%; height: 100%; }
    </style>
    <script>
        window.INNERNET_APP_BASE = "{$app_base_js}";
        window.INNERNET_CONTENT_BASE = "{$content_base_js}";
    </script>
</head>
<body>
    <div id="root"></div>
    <script src="InnerNet.js"></script>
</body>
</html>
HTML;

    echo $html; // phpcs:ignore WordPress.Security.EscapeOutput.OutputNotEscaped
    exit;
}
add_action('template_redirect', 'innernet_webapp_template_redirect', 0);
