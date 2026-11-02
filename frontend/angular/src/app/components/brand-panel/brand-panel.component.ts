import { Component } from '@angular/core';

/**
 * The blue panel beside the sign-in and sign-up forms: the logo and a short
 * description of what Clientele does.
 */
@Component({
  selector: 'app-brand-panel',
  templateUrl: './brand-panel.component.html',
  styleUrls: ['./brand-panel.component.scss']
})
export class BrandPanelComponent {
  highlights = [
    { icon: 'pi-search', text: 'Search, filter and sort every customer' },
    { icon: 'pi-shield', text: 'Roles decide who can change which records' },
    { icon: 'pi-chart-bar', text: 'A dashboard built from the live records' }
  ];
}
