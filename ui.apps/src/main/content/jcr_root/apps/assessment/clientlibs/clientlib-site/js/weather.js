/**
 * Weather Component Client Enhancement
 * 
 * This script provides progressive enhancement for the weather component
 * without exposing API keys or making direct external API calls.
 * All sensitive operations are handled server-side.
 */
(function(window, document) {
    'use strict';

    var WeatherComponent = {
        
        init: function() {
            this.bindEvents();
            this.enhanceComponents();
        },
        
        bindEvents: function() {
            // Listen for dynamic content updates
            document.addEventListener('DOMContentLoaded', this.enhanceComponents.bind(this));
            
            // Listen for AEM component updates (if applicable)
            if (window.addEventListener) {
                window.addEventListener('load', this.enhanceComponents.bind(this));
            }
        },
        
        enhanceComponents: function() {
            var weatherComponents = document.querySelectorAll('.cmp-weather');
            
            weatherComponents.forEach(this.enhanceComponent.bind(this));
        },
        
        enhanceComponent: function(component) {
            var enhancementTrigger = component.querySelector('.cmp-weather__client-enhancement');
            
            if (!enhancementTrigger) {
                return;
            }
            
            var city = enhancementTrigger.getAttribute('data-weather-city');
            var isHealthy = enhancementTrigger.getAttribute('data-healthy') === 'true';
            
            // Add CSS classes for styling
            component.classList.add('cmp-weather--enhanced');
            
            if (isHealthy) {
                component.classList.add('cmp-weather--healthy');
            } else {
                component.classList.add('cmp-weather--unhealthy');
            }
            
            // Add city data for potential client-side features
            if (city) {
                component.setAttribute('data-city', city);
            }
            
            // Hide the enhancement trigger
            enhancementTrigger.style.display = 'none';
            
            // Add refresh functionality (if needed)
            this.addRefreshFunctionality(component);
        },
        
        addRefreshFunctionality: function(component) {
            var refreshButton = document.createElement('button');
            refreshButton.className = 'cmp-weather__refresh';
            refreshButton.innerHTML = 'Refresh';
            refreshButton.setAttribute('aria-label', 'Refresh weather data');
            refreshButton.setAttribute('type', 'button');
            
            refreshButton.addEventListener('click', function(e) {
                e.preventDefault();
                this.refreshWeatherData(component);
            }.bind(this));
            
            // Insert refresh button after the title
            var title = component.querySelector('h2');
            if (title && title.nextSibling) {
                title.parentNode.insertBefore(refreshButton, title.nextSibling);
            }
        },
        
        refreshWeatherData: function(component) {
            component.classList.add('cmp-weather--loading');
            
            // Show loading state
            var loadingElement = component.querySelector('.cmp-weather__loading');
            var serverElement = component.querySelector('.cmp-weather__server');
            var errorElement = component.querySelector('.cmp-weather__error');
            
            if (loadingElement) {
                loadingElement.removeAttribute('hidden');
            }
            if (serverElement) {
                serverElement.style.display = 'none';
            }
            if (errorElement) {
                errorElement.style.display = 'none';
            }
            
            // Refresh the page to get fresh data from server
            // In a real implementation, you might use AJAX to call a server endpoint
            setTimeout(function() {
                window.location.reload();
            }, 1000);
        },
        
        // Utility function to format weather data if needed
        formatWeatherData: function(weatherJson) {
            try {
                var weatherData = JSON.parse(weatherJson);
                
                // Example formatting - customize based on actual API response
                var formatted = {
                    temperature: weatherData.temperature || 'N/A',
                    description: weatherData.description || weatherData.wind || 'N/A',
                    city: weatherData.city || 'Unknown'
                };
                
                return formatted;
            } catch (e) {
                console.warn('Failed to parse weather data:', e);
                return null;
            }
        }
    };

    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function() {
            WeatherComponent.init();
        });
    } else {
        WeatherComponent.init();
    }

    // Expose for potential external use
    window.WeatherComponent = WeatherComponent;

})(window, document);